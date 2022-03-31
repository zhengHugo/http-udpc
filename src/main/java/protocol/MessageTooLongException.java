package protocol;

public class MessageTooLongException extends Exception {

  public MessageTooLongException(String message) {
    super(message);
  }

}
