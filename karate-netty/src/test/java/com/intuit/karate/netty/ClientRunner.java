package com.intuit.karate.netty;

import com.intuit.karate.junit4.Karate;
import cucumber.api.CucumberOptions;
import org.junit.runner.RunWith;

/**
 *
 * @author pthomas3
 */
@RunWith(Karate.class)
@CucumberOptions(features = "classpath:com/intuit/karate/netty/client.feature")
public class ClientRunner {
    
}
