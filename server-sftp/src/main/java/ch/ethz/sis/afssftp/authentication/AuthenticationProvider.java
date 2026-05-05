package ch.ethz.sis.afssftp.authentication;

public interface AuthenticationProvider
{
    String login(String userId, String password);
}
