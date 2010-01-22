package org.basex.query.item;

import static org.basex.query.QueryText.*;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Expr;
import org.basex.query.expr.Return;
import org.basex.query.iter.Iter;
import org.basex.query.iter.SeqIter;
import org.basex.query.util.Err;

/**
 * Stores a sequence type definition.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class SeqType {
  /** Extended type info. */
  public QNm ext;
  /** Sequence type. */
  public Type type;
  /** Occurrence mode: 0 = default, 1 = "?", 2 = "+", 3 = "*". */
  public int occ;

  /**
   * Constructor.
   * @param name sequence type
   * @param o occurrences
   * @param e extended info
   */
  public SeqType(final QNm name, final int o, final boolean e) {
    type = Type.find(name, e);
    occ = o;
  }

  /**
   * Checks the instance of the specified iterator.
   * @param iter iteration to be checked
   * @return result of check
   * @throws QueryException query exception
   */
  public boolean instance(final Iter iter) throws QueryException {
    Item it = iter.next();
    if(it == null) return type == Type.EMP || occ % 2 != 0;
    if(occ < 2) return iter.next() == null && it.type.instance(type) &&
      checkInstance(it);

    do {
      if(!it.type.instance(type) || !checkInstance(it)) return false;
    } while((it = iter.next()) != null);
    return true;
  }

  /**
   * Casts the specified item.
   * @param it item
   * @param expr expression reference
   * @param ctx query context
   * @return resulting item
   * @throws QueryException query exception
   */
  public Item cast(final Item it, final Expr expr, final QueryContext ctx)
      throws QueryException {

    if(it == null) {
      if(occ == 0) Err.empty(expr);
      return null;
    }
    // test to disallow "xs:QName(xs:string(...))"
    if(it.type == type) {
      if(it.type == Type.STR) ((Str) it).direct = false;
      return it;
    }
    return check(type.e(it, ctx));
  }

  /**
   * Casts the specified item.
   * @param item item to be cast
   * @param ctx query context
   * @return resulting item
   * @throws QueryException query exception
   */
  public Item cast(final Item item, final QueryContext ctx)
      throws QueryException {

    final Iter iter = item.iter();
    Item it = iter.next();
    if(it == null) {
      if(type == Type.EMP || occ % 2 != 0) return Seq.EMPTY;
      Err.cast(type, item);
    }
    if(type == Type.EMP) Err.cast(type, item);

    boolean ins = it.type.instance(type);
    if(!it.u() && !ins &&
      // implicit type promotions
      (it.type != Type.DEC || type != Type.FLT && type != Type.DBL) &&
      (it.type != Type.URI || type != Type.STR)) Err.cast(type, it);

    it = check(ins ? it : type.e(it, ctx));
    Item n = iter.next();
    if(occ < 2 && n != null) Err.cast(type, item);

    final SeqIter si = new SeqIter();
    si.add(it);
    while(n != null) {
      ins = n.type.instance(type);
      if(!n.u() && !ins) Err.cast(type, n);
      si.add(check(ins ? n : type.e(n, ctx)));
      n = iter.next();
    }
    return si.finish();
  }

  /**
   * Checks the sequence extension.
   * @param it item
   * @return same item
   * @throws QueryException query exception
   */
  private Item check(final Item it) throws QueryException {
    if(!checkInstance(it)) Err.or(XPCAST, it.type, ext);
    return it;
  }

  /**
   * Checks the sequence extension.
   * @param it item
   * @return same item
   */
  private boolean checkInstance(final Item it) {
    return ext == null || ext.eq(((Nod) it).qname());
  }

  /**
   * Indicates the return type of an expression.
   * Called by the compiler to check if expressions can be reformulated.
   * null is returned by default.
   * @return result of check
   */
  public Return returned() {
    final Return r = type.returned();
    return occ == 0 ? r : r == Return.NUM ? Return.NUMSEQ :
      r == Return.NOD ? Return.NODSEQ : Return.NONUMSEQ;
  }

  @Override
  public String toString() {
    return type + (occ == 0 ? "" : occ == 1 ? "?" : occ == 2 ? "+" : "*");
  }
}