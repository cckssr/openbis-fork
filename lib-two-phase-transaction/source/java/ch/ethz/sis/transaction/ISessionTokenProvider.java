package ch.ethz.sis.transaction;

public interface ISessionTokenProvider
{
    boolean isValid(String sessionToken);

    boolean isInstanceAdminOrSystem(String sessionToken);
}
