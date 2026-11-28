package cl.trato.identity.application.port;

/** Comprueba códigos CUT y la pertenencia de una comuna a una región. */
public interface ProfileLocationValidator {

  boolean isValid(String regionCode, String communeCode);
}
