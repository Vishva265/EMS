package in.iaip.ems.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AES-GCM: every value gets a fresh nonce; the key stays outside the database. */
@Component
public class IdentityVault {
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  public IdentityVault(@Value("${app.encryption-key}") String base64Key) {
    byte[] bytes = Base64.getDecoder().decode(base64Key);
    if (bytes.length != 32)
      throw new IllegalStateException("ENCRYPTION_KEY must be a Base64-encoded 32-byte key");
    key = new SecretKeySpec(bytes, "AES");
  }

  public String encrypt(String value) {
    try {
      byte[] nonce = new byte[12];
      random.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
      return "v1:"
          + Base64.getEncoder().encodeToString(nonce)
          + ":"
          + Base64.getEncoder()
              .encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to protect identity information");
    }
  }

  public String decrypt(String value) {
    try {
      String[] parts = value.split(":");
      if (!parts[0].equals("v1")) throw new IllegalStateException();
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key,
          new GCMParameterSpec(128, Base64.getDecoder().decode(parts[1])));
      return new String(
          cipher.doFinal(Base64.getDecoder().decode(parts[2])), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read protected identity information");
    }
  }
}
