Feature: testing binary response handling

Scenario: get binary result and make sure it hasn't been corrupted
  * def checkBinaryResult =
  """
  function(b){
    var Runner = Java.type('com.intuit.karate.mock.MockServerTest')
    var expected =  Runner.testBytes;
    var FileUtils = Java.type('com.intuit.karate.FileUtils');
    var actualBytes = FileUtils.toBytes(b);
    print('expected byte count: ' + expected.length + ', response byte count: ' + actualBytes.length);
    return(expected.length == actualBytes.length)
  }
  """
Given url mockServerUrl
And path 'binary'
When method get
Then status 200
Then assert checkBinaryResult(response)
