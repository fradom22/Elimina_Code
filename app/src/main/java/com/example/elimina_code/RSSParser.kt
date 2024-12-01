import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.rometools.rome.feed.synd.SyndEntry
import java.net.URL

object RSSParser {

    // Funzione per ottenere le notizie dal feed RSS
    fun getNewsFromFeed(feedUrl: String): List<String> {
        return try {
            // Creiamo un URL per il feed
            val url = URL(feedUrl)

            // Creiamo un oggetto SyndFeedInput per leggere il feed RSS
            val input = SyndFeedInput()
            val feed = input.build(XmlReader(url))

            // Estrarre i titoli delle notizie dal feed e restituirli come una lista di stringhe
            feed.entries.map { it.title } // Restituisce solo i titoli delle notizie
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()  // Restituisce una lista vuota in caso di errore
        }
    }
}
