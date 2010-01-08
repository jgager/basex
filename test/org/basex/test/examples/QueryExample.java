package org.basex.test.examples;

import java.io.IOException;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.proc.Close;
import org.basex.core.proc.CreateDB;
import org.basex.core.proc.DropDB;
import org.basex.core.proc.XQuery;
import org.basex.data.Result;
import org.basex.data.XMLSerializer;
import org.basex.io.PrintOutput;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.query.item.Item;
import org.basex.query.iter.Iter;

/**
 * This class contains several variants of XQuery processing in BaseX.
 * For further information on BaseX Client-Side
 * abilities in XQuery Processing please see:
 * @see XQueryExample#main(String[])
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author BaseX Team
 */
public final class QueryExample {
  /** The database context. */
  private Context context;
  /** Output stream, initialized by the constructor. */
  private PrintOutput out;

  /**
   * Starts the QueryExample from the command line.
   * @param args (ignored) command-line arguments
   */
  public static void main(final String[] args) {
    try {
      new QueryExample().run();
    } catch(final IOException e) {
      e.printStackTrace();
    } catch(final BaseXException e) {
      System.err.println(e.getMessage());
    }
  }

  /**
   * Constructor, initializing the database context and the output stream.
   */
  private QueryExample() {
    context = new Context();
    out = new PrintOutput(System.out);
    // Alternative: write results to disk
    // out = new PrintOutput("result.txt");
  }

  /**
   * Runs the example queries.
   * @throws IOException if an error occurs while serializing the results
   * @throws BaseXException if a database command fails for any reason
   */
  private void run() throws IOException, BaseXException {
    // Create a database from the specified file.
    System.out.println("\n=== I Create a database from a file.");
    new CreateDB("input.xml", "Example1").exec(context, out);

    // -------------------------------------------------------------------------
    // Evaluate the specified query
    System.out.println("\n=== II Evaluating queries.");
    String query = "for $x in //body//li return $x";

    // -------------------------------------------------------------------------
    // Process the query by a simple call of the query command
    try {
      System.out.println("===== XQuery Proc: direct output.");
      queryExample(query);

      // uncomment the following line to see error handling.
      // queryExample("for error s$x in . return $x");
    } catch(final Exception e) {
      System.err.println(e.getMessage());
    }

    // -------------------------------------------------------------------------
    // Iterate through all single results
    try {
      System.out.println("\n===== XQuery Node Iteration and XML Serializing.");
      iterateExample(query);
    } catch(final Exception e) {
      System.err.println(e.getMessage());
    }

    // -------------------------------------------------------------------------
    // Process the whole result instance at once
    try {
      System.out.println("\n=== Serializing a complete result instance.");
      resultExample(query);
    } catch(final Exception e) {
      System.err.println(e.getMessage());
    }

    // -------------------------------------------------------------------------
    // Close and drop the database
    new Close().exec(context, out);
    new DropDB("Example1").exec(context, out);

    // -------------------------------------------------------------------------
    // Close the output stream
    out.close();
  }

  /**
   * This method executes an XQuery process for the given database context.
   * The results are automatically serialized and printed to a specified
   * output stream.
   *
   * @param query query to be evaluated
   * @throws BaseXException if a database command fails for any reason
   */
  private void queryExample(final String query) throws BaseXException {
    new XQuery(query).exec(context, out);
  }

  /**
   * Shows how results can be iterated and serialized one after another using
   * the {@link QueryProcessor} class. This is especially useful if you happen
   * to have very big results, as you will not have to process all resulting
   * nodes at once.
   *
   * @param query query to be evaluated
   * @throws QueryException if an error occurs while evaluating the query
   * @throws IOException if an error occurs while serializing the results
   */
  private void iterateExample(final String query)
      throws QueryException, IOException {

    // -------------------------------------------------------------------------
    // Create a QueryProcessor
    final QueryProcessor qp = new QueryProcessor(query, context);

    // -------------------------------------------------------------------------
    // Store the pointer to the result in an iterator:
    final Iter iter = qp.iter();
    Item item;

    // -------------------------------------------------------------------------
    // Create an XML serializer
    XMLSerializer serializer = new XMLSerializer(out);
    
    // -------------------------------------------------------------------------
    // Iterate through all items and serialize contents
    while(null != (item = iter.next())) {
      item.serialize(serializer);
    }

    // -------------------------------------------------------------------------
    // Close the serializer
    serializer.close();
  }

  /**
   * This method uses the {@link QueryProcessor} to evaluate a query.
   * @param query query to be evaluated
   * @throws QueryException if an error occurs while evaluating the query
   * @throws IOException if an error occurs while serializing the results
   */
  private void resultExample(final String query)
      throws QueryException, IOException {

    // Create and execute a query
    final QueryProcessor processor = new QueryProcessor(query, context);

    // -------------------------------------------------------------------------
    // Execute the query.
    final Result result = processor.query();

    // -------------------------------------------------------------------------
    // Create an XML serializer
    XMLSerializer serializer = new XMLSerializer(out);
    
    // Serialize all results
    result.serialize(serializer);

    // -------------------------------------------------------------------------
    // Close the serializer
    serializer.close();

    // -------------------------------------------------------------------------
    // Close the query processor
    processor.close();
  }
}
