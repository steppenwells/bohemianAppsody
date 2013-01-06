import com.swells.sonas._
import model.Indexes
import org.scalatra._
import javax.servlet.ServletContext

/**
 * This is the Scalatra bootstrap file. You can use it to mount servlets or
 * filters. It's also a good place to put initialization code which needs to
 * run at application start (e.g. database configurations), and init params.
 */
class Scalatra extends LifeCycle {
  override def init(context: ServletContext) {

    try {Indexes.init} catch {case e => e.printStackTrace()}

    // Mount one or more servlets
    context.mount(new UI, "/*")
  }
}
