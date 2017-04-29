package mezi.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LangList extends LinkedList<Object> {


  private static final long serialVersionUID = 8058878112327292400L;

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(LangList.class);

  public LangList() {
		super();
	}

	public LangList(Stream<Object> stream){
		super();
		//stream.forEach((ele)->this.add(ele));
		Iterator<Object> it = stream.iterator();

		while(it.hasNext()) {
			add(it.next());
		}
	}

	public void dump() {
		int size = this.size();
		for( int i = 0 ; i < size; i++) {
			System.out.println(this.get(i));
		}

	}


	public LangList __plus__(LangList rlist) {
		LangList newlist = new LangList();

		int size = this.size();
		for( int i = 0 ; i < size ; i++) {
			newlist.add( this.get(i));
		}

		if( rlist != null )	{
			size = rlist.size();
			for( int i = 0 ; i < size ; i++) {
				newlist.add( rlist.get(i));
			}
		}

		return newlist;
	}


	public Object __map__(int i) {
		return get(i);
	}

}
