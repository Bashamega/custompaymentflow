package com.stripe.sample;

import java.util.HashMap;
import java.nio.file.Paths;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.port;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import com.stripe.Stripe;
import com.stripe.net.ApiResource;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.exception.*;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

import io.github.cdimascio.dotenv.Dotenv;

public class Server {
    private static Gson gson = new Gson();

    static class CreatePaymentRequest {
        @SerializedName("paymentMethodType")
        String paymentMethodType;

        @SerializedName("currency")
        String currency;

        public String getPaymentMethodType() {
            return paymentMethodType;
        }

        public String getCurrency() {
            return currency;
        }
    }

    static class ConfigResponse {
        private String publishableKey;

        public ConfigResponse(String publishableKey) {
            this.publishableKey = publishableKey;
        }
    }

    static class FailureResponse {
        private HashMap<String, String> error;

        public FailureResponse(String message) {
            this.error = new HashMap<String, String>();
            this.error.put("message", message);
        }
    }

    static class CreatePaymentResponse {
        private String clientSecret;

        public CreatePaymentResponse(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    public static void main(String[] args) {
        port(4242);
        Dotenv dotenv = Dotenv.load();

        Stripe.apiKey = dotenv.get("STRIPE_SECRET_KEY");

        // For sample support and debugging, not required for production:
        Stripe.setAppInfo(
            "stripe-samples/accept-a-payment/custom-payment-flow",
            "0.0.1",
            "https://github.com/stripe-samples"
        );

        staticFiles.externalLocation(
          Paths.get(
            Paths.get("").toAbsolutePath().toString(),
            dotenv.get("STATIC_DIR")
          ).normalize().toString());

        get("/config", (request, response) -> {
            response.type("application/json");

            return gson.toJson(new ConfigResponse(dotenv.get("STRIPE_PUBLISHABLE_KEY")));
        });

        post("/create-payment-intent", (request, response) -> {
            response.type("application/json");

            CreatePaymentRequest postBody = gson.fromJson(request.body(), CreatePaymentRequest.class);

            PaymentIntentCreateParams.Builder paramsBuilder = new PaymentIntentCreateParams
              .Builder()
              .addPaymentMethodType(postBody.getPaymentMethodType())
              .setCurrency(postBody.getCurrency())
              .setAmount(5999L);

             if(postBody.getPaymentMethodType().equals("link")){
                paramsBuilder.addPaymentMethodType("card");
                paramsBuilder.addPaymentMethodType("link");
             }

            // If this is for an ACSS payment, we add payment_method_options to create
            // the Mandate.
            System.out.println(postBody.getPaymentMethodType());
            if(postBody.getPaymentMethodType().equals("acss_debit")) {
                paramsBuilder.setPaymentMethodOptions(
                    PaymentIntentCreateParams.PaymentMethodOptions
                      .builder()
                      .setAcssDebit(PaymentIntentCreateParams
                        .PaymentMethodOptions
                        .AcssDebit
                        .builder()
                        .setMandateOptions(PaymentIntentCreateParams
                            .PaymentMethodOptions
                            .AcssDebit
                            .MandateOptions
                            .builder()
                            .setPaymentSchedule(PaymentIntentCreateParams.PaymentMethodOptions.AcssDebit.MandateOptions.PaymentSchedule.SPORADIC)
                            .setTransactionType(PaymentIntentCreateParams.PaymentMethodOptions.AcssDebit.MandateOptions.TransactionType.PERSONAL)
                            .build())
                        .build())
                      .build());
            }

            PaymentIntentCreateParams createParams = paramsBuilder.build();

            try {
              // Create a PaymentIntent with the order amount and currency
              PaymentIntent intent = PaymentIntent.create(createParams);

              // Send PaymentIntent details to client
              return gson.toJson(new CreatePaymentResponse(intent.getClientSecret()));
            } catch(StripeException e) {
              response.status(400);
              return gson.toJson(new FailureResponse(e.getMessage()));
            } catch(Exception e) {
              response.status(500);
              return gson.toJson(e);
            }
        });

        get("/payment/next", (request, response) -> {
            PaymentIntent intent =PaymentIntent.retrieve(request.queryParams("payment_intent"));
            response.redirect("/success?payment_intent_client_secret=" + intent.getClientSecret());
            return "";
        });

        get("/success", (request, response) -> {
            response.redirect("/success.html");
            return "";
        });

        post("/webhook", (request, response) -> {
            String payload = request.body();
            String sigHeader = request.headers("Stripe-Signature");
            String endpointSecret = dotenv.get("STRIPE_WEBHOOK_SECRET");

            Event event = null;

            try {
                event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            } catch (SignatureVerificationException e) {
                // Invalid signature
                response.status(400);
                return "";
            }

            switch (event.getType()) {
            case "payment_intent.succeeded":
                // Fulfill any orders, e-mail receipts, etc
                // To cancel the payment you will need to issue a Refund
                // (https://stripe.com/docs/api/refunds)
                System.out.println("💰Payment received!");
                break;
            case "payment_intent.payment_failed":
                System.out.println("❌ Payment failed.");
                break;
            default:
                // Unexpected event type
                response.status(400);
                return "";
            }

            response.status(200);
            return "";
        });
    }
}
