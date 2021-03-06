package org.http4s.blaze.http.http_parser;

public class BaseExceptions {

    public static abstract class ParserException extends Exception {

        public ParserException(String msg) {
            super(msg);
        }

        public final String msg() {
            return this.getMessage();
        }

        // We will be using this for
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    public static class BadRequest extends ParserException {
        public BadRequest(String msg) {
            super(msg);
        }
    }

    public static class BadResponse extends ParserException {
        public BadResponse(String msg) {
            super(msg);
        }
    }

    public static class InvalidState extends ParserException {
        public InvalidState(String msg) {
            super(msg);
        }
    }
}
