package coffeescript.nb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.EmbeddingProvider;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

/**
 * 
 * @author Denis Stepanov
 */
public class CoffeeScriptEmbeddingProvider extends EmbeddingProvider {

    @Override
    public List<Embedding> getEmbeddings(Snapshot snapshot) {
        List<Embedding> embeddings = new ArrayList<Embedding>();
        return embeddings;
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public void cancel() {
    }

    public static final class Factory extends TaskFactory {

        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
//            if (snapshot.getSource().getMimeType().equals(CoffeeScriptLanguage.MIME_TYPE)) {
//                return Collections.singleton(new CoffeeScriptEmbeddingProvider());
//            }
            return Collections.emptyList();
        }
    }
}
