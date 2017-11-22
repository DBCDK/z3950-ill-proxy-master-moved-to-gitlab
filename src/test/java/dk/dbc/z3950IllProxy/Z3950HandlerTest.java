package dk.dbc.z3950IllProxy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class Z3950HandlerTest {
    private Z3950TestUtils z3950TestUtils = new Z3950TestUtils();
    private ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void testMarshalling() throws IOException {
        z3950TestUtils.printTestEntry();
        String input = "[{\"url\":\"webservice.statsbiblioteket.dk:9054/standard\",\"server\":\"webservice.statsbiblioteket.dk\",\"port\":9054,\"base\":\"standard\",\"id\":\"4874555\",\"user\":\"\",\"group\":\"\",\"password\":\"\",\"format\":\"XML\",\"esn\":\"B3\",\"schema\":\"1.2.840.10003.13.7.4\",\"responder\":\"820010\"},{\"url\":\"z3950.lolland.integrabib.dk\",\"server\":\"z3950.lolland.integrabib.dk\",\"port\":210,\"base\":\"default\",\"id\":\"07197195\",\"user\":\"\",\"group\":\"\",\"password\":\"\",\"format\":\"XML\",\"esn\":\"B3\",\"schema\":\"1.2.840.10003.13.7.4\",\"responder\":\"736000\"}]";
        List<Z3950HoldingsRequest> z3950HoldingsRequests = objectMapper.readValue(input, new TypeReference<List<Z3950HoldingsRequest>>(){});
        assertThat(z3950HoldingsRequests, is(notNullValue()));
        assertThat(z3950HoldingsRequests.size(), is(2));
        assertThat(z3950HoldingsRequests.get(0).getUrl(), anyOf(is("webservice.statsbiblioteket.dk:9054/standard"), is("z3950.lolland.integrabib.dk")));
        assertThat(z3950HoldingsRequests.get(0).getServer(), anyOf(is("webservice.statsbiblioteket.dk"), is("z3950.lolland.integrabib.dk")));
        assertThat(z3950HoldingsRequests.get(0).getPort(), anyOf(is(9054), is(210)));
        assertThat(z3950HoldingsRequests.get(0).getBase(), anyOf(is("standard"), is("default")));
        assertThat(z3950HoldingsRequests.get(0).getId(), anyOf(is("4874555"), is("07197195")));
        assertThat(z3950HoldingsRequests.get(0).getUser(), is(""));
        assertThat(z3950HoldingsRequests.get(0).getGroup(), is(""));
        assertThat(z3950HoldingsRequests.get(0).getPassword(), is(""));
        assertThat(z3950HoldingsRequests.get(0).getFormat(), is("XML"));
        assertThat(z3950HoldingsRequests.get(0).getEsn(), is("B3"));
        assertThat(z3950HoldingsRequests.get(0).getSchema(), is("1.2.840.10003.13.7.4"));
        assertThat(z3950HoldingsRequests.get(0).getResponder(), anyOf(is("820010"), is("736000")));
        assertThat(z3950HoldingsRequests.get(1).getUrl(), anyOf(is("webservice.statsbiblioteket.dk:9054/standard"), is("z3950.lolland.integrabib.dk")));
        assertThat(z3950HoldingsRequests.get(1).getServer(), anyOf(is("webservice.statsbiblioteket.dk"), is("z3950.lolland.integrabib.dk")));
        assertThat(z3950HoldingsRequests.get(1).getPort(), anyOf(is(9054), is(210)));
        assertThat(z3950HoldingsRequests.get(1).getBase(), anyOf(is("standard"), is("default")));
        assertThat(z3950HoldingsRequests.get(1).getId(), anyOf(is("4874555"), is("07197195")));
        assertThat(z3950HoldingsRequests.get(1).getUser(), is(""));
        assertThat(z3950HoldingsRequests.get(1).getGroup(), is(""));
        assertThat(z3950HoldingsRequests.get(1).getPassword(), is(""));
        assertThat(z3950HoldingsRequests.get(1).getFormat(), is("XML"));
        assertThat(z3950HoldingsRequests.get(1).getEsn(), is("B3"));
        assertThat(z3950HoldingsRequests.get(1).getSchema(), is("1.2.840.10003.13.7.4"));
        assertThat(z3950HoldingsRequests.get(1).getResponder(), anyOf(is("820010"), is("736000")));
        String output = objectMapper.writeValueAsString(z3950HoldingsRequests);
        assertThat(output, is(input));
    }
}
