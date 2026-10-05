import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING_ACTIVATION';

/** Usuário da empresa, como devolvido por GET /api/users. */
export interface UserSummary {
  id: string;
  name: string;
  email: string;
  profileId: string;
  profileName: string | null;
  twoFactorEnabled: boolean;
  status: UserStatus;
}

/** Perfil de acesso, como devolvido por GET /api/profiles. */
export interface ProfileOption {
  id: string;
  name: string;
}

/** Corpo do POST /api/users. */
export interface NewUser {
  name: string;
  email: string;
  password: string;
  profileId: string;
}

export type UsersLoadStatus = 'idle' | 'loading' | 'ready' | 'error';

/** Mensagem do backend para uma falha (`{message}`), se houver. */
export function userFailureDetail(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return '';
  const message = error.error?.message;
  return typeof message === 'string' ? message.trim() : '';
}

/** O backend recusa o cadastro de um e-mail que já existe com 400 `Email already registered: …`. */
export function isDuplicateEmail(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 400 && /already registered/i.test(userFailureDetail(error));
}

/** Usuários e perfis da empresa do chamador (GET/POST/PATCH /api/users, GET /api/profiles). */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly usersUrl = `${environment.apiUrl}/users`;
  private readonly profilesUrl = `${environment.apiUrl}/profiles`;

  private readonly userList = signal<UserSummary[]>([]);
  private readonly profileList = signal<ProfileOption[]>([]);
  private readonly loadStatus = signal<UsersLoadStatus>('idle');

  readonly users = this.userList.asReadonly();
  readonly profiles = this.profileList.asReadonly();
  readonly status = this.loadStatus.asReadonly();

  /** Carrega usuários e perfis. Nunca rejeita: o resultado fica em `status`. */
  async load(): Promise<void> {
    this.loadStatus.set('loading');
    try {
      const [users, profiles] = await Promise.all([
        firstValueFrom(this.http.get<UserSummary[]>(this.usersUrl)),
        firstValueFrom(this.http.get<ProfileOption[]>(this.profilesUrl)),
      ]);
      this.userList.set(users);
      this.profileList.set([...profiles].sort((a, b) => a.name.localeCompare(b.name, 'pt-BR')));
      this.loadStatus.set('ready');
    } catch {
      this.loadStatus.set('error');
    }
  }

  /** POST /api/users e, em seguida, relê a lista: o novo usuário entra com os valores reais do servidor (2FA, status). */
  async register(user: NewUser): Promise<void> {
    await firstValueFrom(this.http.post<{ id: string }>(this.usersUrl, user));
    this.userList.set(await firstValueFrom(this.http.get<UserSummary[]>(this.usersUrl)));
  }

  /** PATCH /api/users/{id} com `{status: 'INACTIVE'}`; a linha muda de status no lugar, sem recarregar a lista. */
  async deactivate(id: string): Promise<void> {
    await firstValueFrom(this.http.patch<void>(`${this.usersUrl}/${id}`, { status: 'INACTIVE' }));
    this.userList.update(list => list.map(u => (u.id === id ? { ...u, status: 'INACTIVE' } : u)));
  }
}
