import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserService, UserSummary, isDuplicateEmail, userFailureDetail } from './user.service';

const user = (over: Partial<UserSummary>): UserSummary => ({
  id: 'u1', name: 'Ana', email: 'ana@acme.com', profileId: 'p1', profileName: 'Administrator',
  twoFactorEnabled: true, status: 'ACTIVE', ...over,
});

describe('UserService', () => {
  let service: UserService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(UserService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('loads users and profiles together, profiles sorted by name', async () => {
    const loading = service.load();
    expect(service.status()).toBe('loading');
    http.expectOne('/api/users').flush([user({})]);
    http.expectOne('/api/profiles').flush([{ id: 'b', name: 'Salesperson' }, { id: 'a', name: 'Administrator' }]);
    await loading;

    expect(service.status()).toBe('ready');
    expect(service.users().map(u => u.name)).toEqual(['Ana']);
    expect(service.profiles().map(p => p.name)).toEqual(['Administrator', 'Salesperson']);
  });

  it('reports a failed load without rejecting', async () => {
    const loading = service.load();
    http.expectOne('/api/users').flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/profiles').flush([]);
    await loading;

    expect(service.status()).toBe('error');
  });

  it('posts a new user and then re-reads the list so it carries the server values', async () => {
    const registering = service.register({ name: 'Bia', email: 'bia@acme.com', password: 'x', profileId: 'p2' });
    const post = http.expectOne('/api/users');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'Bia', email: 'bia@acme.com', password: 'x', profileId: 'p2' });
    post.flush({ id: 'u2' });
    await Promise.resolve();
    http.expectOne('/api/users').flush([user({}), user({ id: 'u2', name: 'Bia', email: 'bia@acme.com' })]);
    await registering;

    expect(service.users().map(u => u.name)).toEqual(['Ana', 'Bia']);
  });

  it('inactivates with PATCH {status: INACTIVE} and flips only that row in place', async () => {
    const loading = service.load();
    http.expectOne('/api/users').flush([user({}), user({ id: 'u2', name: 'Bia' })]);
    http.expectOne('/api/profiles').flush([]);
    await loading;

    const patching = service.deactivate('u2');
    const patch = http.expectOne('/api/users/u2');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ status: 'INACTIVE' });
    patch.flush(null, { status: 204, statusText: 'No Content' });
    await patching;

    expect(service.users().map(u => u.status)).toEqual(['ACTIVE', 'INACTIVE']);
  });

  it('recognises the backend duplicate-e-mail rejection and reads its message', () => {
    const duplicate = new HttpErrorResponse({ status: 400, error: { message: 'Email already registered: a@b.com' } });
    const other = new HttpErrorResponse({ status: 400, error: { message: 'Name is required' } });

    expect(isDuplicateEmail(duplicate)).toBeTrue();
    expect(isDuplicateEmail(other)).toBeFalse();
    expect(userFailureDetail(other)).toBe('Name is required');
    expect(userFailureDetail(new Error('x'))).toBe('');
  });
});
