package com.example.hms.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.servlet.ModelAndView;

/** Shared, safe text for MVC exceptions and servlet error dispatches. */
public final class ErrorPages {
    private ErrorPages() { }

    public static ModelAndView create(int status, String title, String message) {
        ModelAndView view = new ModelAndView("error/message");
        view.setStatus(HttpStatusCode.valueOf(status));
        view.addObject("statusCode", status);
        view.addObject("title", title);
        view.addObject("message", message);
        return view;
    }

    public static ModelAndView standard(int status) {
        return switch (status) {
            case 400 -> create(400, "Invalid request", "Check the form values, record number, page number, and selected filters, then try again.");
            case 404 -> create(404, "Page not found", "This page or record could not be found. Use the navigation to return to your workspace.");
            case 405 -> create(405, "Action not supported", "This page does not accept that action. Open the appropriate form and submit it using its button.");
            case 406 -> create(406, "Response format not supported", "Open this page in a web browser to continue.");
            case 409 -> create(409, "Record changed", "Another request is changing this record. Reload it and check its current state before trying again.");
            case 415 -> create(415, "Request format not supported", "Use the application form to submit this information.");
            case 503 -> create(503, "Service temporarily unavailable", "The database is temporarily unavailable. Try again shortly. Before resubmitting a payment, check the invoice's payment history.");
            default -> status >= 400 && status < 500
                    ? create(status, "Unable to process this request", "Use the navigation to return to your workspace and try the action again.")
                    : create(500, "Something went wrong", "We could not complete this request. Return to the workspace and check the record before trying again. For payments, check payment history before resubmitting.");
        };
    }
}
