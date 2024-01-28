package tla.backend.es;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ConnectionTest {

    @Test
    void envSet() {
        String esPort = System.getenv("ES_PORT");
        assertNotNull(esPort, "ES_PORT should be set");
    }

    private URL getElasticsearchURL(String path) throws MalformedURLException, URISyntaxException {
        return new URI(
            String.format("http://localhost:%s/%s", System.getenv("ES_PORT"), path)
        ).toURL();
    }

    @Test
    void doesESrespond() throws Exception {
        var responseStatus = (
            (HttpURLConnection) getElasticsearchURL("").openConnection()
        ).getResponseCode();
        assertEquals(200, responseStatus, "ES should return HTTP code 200");
    }

}