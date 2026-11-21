terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

data "aws_availability_zones" "available" {
  state = "available"
}

# Latest Canonical Ubuntu 24.04 for ARM (Graviton).
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-arm64-server-*"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
  }
}

# --- Network: one public subnet, no NAT Gateway ---------------------------------

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "${var.name}-vpc" }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
  tags   = { Name = "${var.name}-igw" }
}

resource "aws_subnet" "public" {
  vpc_id            = aws_vpc.this.id
  cidr_block        = var.subnet_cidr
  availability_zone = data.aws_availability_zones.available.names[var.availability_zone_index]

  tags = { Name = "${var.name}-public" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = { Name = "${var.name}-public-rt" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "vm" {
  name        = "${var.name}-vm"
  description = "Gravita homologation VM: HTTP/HTTPS public, SSH from the team only"
  vpc_id      = aws_vpc.this.id

  tags = { Name = "${var.name}-vm" }
}

resource "aws_vpc_security_group_ingress_rule" "web" {
  for_each = toset(["80", "443"])

  security_group_id = aws_security_group.vm.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = tonumber(each.key)
  to_port           = tonumber(each.key)
}

resource "aws_vpc_security_group_ingress_rule" "https_udp" {
  security_group_id = aws_security_group.vm.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "udp"
  from_port         = 443
  to_port           = 443
  description       = "HTTP/3"
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each = toset(var.ssh_allowed_cidrs)

  security_group_id = aws_security_group.vm.id
  cidr_ipv4         = each.key
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.vm.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# --- IAM: the VM's role (the environment attaches SQS/S3 policies to it) ---------

resource "aws_iam_role" "vm" {
  name = "${var.name}-vm"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# Shell access through SSM Session Manager, so port 22 can stay closed.
resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.vm.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "vm" {
  name = "${var.name}-vm"
  role = aws_iam_role.vm.name
}

# --- Instance ----------------------------------------------------------------

resource "aws_instance" "this" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.vm.id]
  iam_instance_profile   = aws_iam_instance_profile.vm.name

  # IMDSv2 only; hop limit 2 so containers (app, backup) can use the instance role.
  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_gbs
    encrypted   = true
  }

  user_data = templatefile("${path.module}/cloud-init.yaml.tftpl", {
    deploy_user     = var.deploy_user
    ssh_public_keys = var.ssh_public_keys
    swap_gbs        = var.swap_gbs
  })

  tags = { Name = "${var.name}-vm" }

  lifecycle {
    # A newer AMI or an edited cloud-init must not silently recreate the VM (and wipe
    # the database volume). Recreate deliberately with `terraform apply -replace`.
    ignore_changes = [ami, user_data]
  }
}

# Fixed public IP, so DNS survives stop/start.
resource "aws_eip" "this" {
  instance = aws_instance.this.id
  domain   = "vpc"

  tags = { Name = "${var.name}-eip" }
}
