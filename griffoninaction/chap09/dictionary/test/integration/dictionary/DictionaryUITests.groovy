package dictionary

import org.uispec4j.*
import griffon.uispec4j.GriffonUISpecTestCase

class DictionaryUiTests extends GriffonUISpecTestCase {
    void testInitialState() {
         not(getMainWindow().getButton('search').isEnabled())
    }

    void testWordIsFound() {
         Window window = getMainWindow()
         window.getTextBox('word').text = 'griffon'
         window.getButton('search').click()
         assertThat(window.getTextBox('result')
                          .textEquals('griffon: Grails inspired desktop application development platform.'))
    }

    void testWordIsNotFound() {
         Window window = getMainWindow()
         window.getTextBox('word').text = 'spock'
         window.getButton('search').click()
         assertThat(window.getTextBox('result')
                          .textEquals("spock: Word doesn't exist in dictionary"))
    }
}
