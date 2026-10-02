package br.gravita.core.usercases.system;

/**
 * Everything the public signup form collects. {@code plan} and {@code billing} are the raw slugs from the
 * pricing page (bronze/silver/gold, monthly/annual); null or blank means "use the default".
 */
public record SignupCommand(String fullName, String email, String rawPassword, String companyName, String cnpj,
		String phone, String plan, String billing) {

	/** Keeps the plaintext password out of any log line that prints the command. */
	@Override
	public String toString() {
		return "SignupCommand[email=" + email + ", companyName=" + companyName + ", plan=" + plan
				+ ", billing=" + billing + "]";
	}
}
