package com.denny.samplews.df;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.joda.time.Duration;
import org.slf4j.LoggerFactory;

public class SampleWS {
    final static org.slf4j.Logger logger = LoggerFactory.getLogger(SampleWS.class);

    public static final boolean local = false;
    private static final String SUBSCRIPTION_TOPIC = "projects/denny-lab-1/subscriptions/sampleWSSubs";
    private static final String SUBSCRIPTION_TOPIC_LOCAL = SUBSCRIPTION_TOPIC;
    private static final String HOST_WS = "10.148.0.5"; // internal kubernetes LB.
    private static final String GS_HOST = "gs://samplews"; // GCS bucket

    public static void main(String[] args) {
        // Create and set your PipelineOptions.
        PipelineOptionsFactory.Builder options = PipelineOptionsFactory.fromArgs(args).withValidation();


        // Create the Pipeline object with the options we defined above.
        Pipeline p = Pipeline.create(options.create());

        // ---------------------------
        PCollection<String> dataStream;

        if (local) {
            dataStream = p.apply("Read PubSub Msg", PubsubIO.readStrings().fromSubscription(SUBSCRIPTION_TOPIC_LOCAL));
        } else {
            dataStream = p.apply("Read PubSub Msg", PubsubIO.readStrings().fromSubscription(SUBSCRIPTION_TOPIC));
        }

        PCollection<String> fixWindow = dataStream.apply("fixed window",
                Window.<String>into(FixedWindows.of(Duration.standardSeconds(5))));

        PCollection<String> wsCall = fixWindow.apply("call WS", ParDo.of(new DoFn<String, String>() {
            @ProcessElement
            public void processElement(ProcessContext c) {
                // call web service
                DefaultHttpClient httpclient = new DefaultHttpClient();
                String callResult = "<no result>";
                try {
                    // specify the host, protocol, and port
                    HttpHost target = new HttpHost(HOST_WS, 80, "http");

                    // specify the get request
                    HttpGet getRequest = new HttpGet("/hello-world");

                    logger.info("executing request to " + target + " URI: is hello-world");

                    HttpResponse httpResponse = httpclient.execute(target, getRequest);
                    HttpEntity entity = httpResponse.getEntity();

                    if (entity != null) {
                        callResult = EntityUtils.toString(entity);
                        logger.info("Result: "+callResult);
                    } else {
                        logger.warn("Entity is null");
                    }

                } catch (Exception e) {
                    logger.error("Error call WS. ", e);
                } finally {
                    httpclient.getConnectionManager().shutdown();
                }

                logger.info("Message from PubSub: "+c.element());
                c.output("WS Call Result is: "+callResult+". Trigger message from PubSub is: "+c.element()+" . ");
            }
        }));

        wsCall.apply("Write Result", TextIO.write().withWindowedWrites().withNumShards(2)
                .to(GS_HOST+"/out")
                .withSuffix(".txt"));


        // ---------------------------

        // Run the pipeline.
        p.run().waitUntilFinish();
    }
}
