package tla.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Brings an auto-configured {@link MockMvc} object.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {App.class})
public abstract class AbstractMockMvcTest {

    @Autowired
    protected MockMvc mockMvc;

}