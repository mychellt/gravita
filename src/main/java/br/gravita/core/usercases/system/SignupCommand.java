package br.gravita.core.usercases.system;

import lombok.Builder;

@Builder
public record SignupCommand(String fullName, String email, String rawPassword, String companyName, String cnpj,
                            String phone, String plan, String billing) {


    @Override
    public String toString() {
        return "SignupCommand[email=" + email + ", companyName=" + companyName + ", plan=" + plan
                + ", billing=" + billing + "]";
    }
}
