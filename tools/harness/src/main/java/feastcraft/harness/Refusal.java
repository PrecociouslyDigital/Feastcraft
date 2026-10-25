package feastcraft.harness;

/** A request the harness won't carry out, with the HTTP status that says why. */
final class Refusal extends RuntimeException {
    final int status;

    private Refusal(int status, String message) {
        super(message);
        this.status = status;
    }

    static Refusal badRequest(String message) {
        return new Refusal(400, message);
    }

    /** The request is fine, but the game isn't in a state where it can be done (no world, wrong screen). */
    static Refusal conflict(String message) {
        return new Refusal(409, message);
    }
}
