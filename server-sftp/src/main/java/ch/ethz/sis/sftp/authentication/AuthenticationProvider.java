package ch.ethz.sis.sftp.authentication;

public interface AuthenticationProvider
{
    String login(String userId, String password);
}
