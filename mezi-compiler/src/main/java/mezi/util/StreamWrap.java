package mezi.util;



import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SuppressWarnings({"rawtypes", "unchecked"})
public class StreamWrap implements java.util.stream.Stream{

	private java.util.stream.Stream stream;

	//public StreamWrap(java.util.stream.Stream src) { this.stream = src; }
	public void initstream(java.util.stream.Stream src) {
	  this.stream = src;
	}

	@Override
	public void close() {
		stream.close();
	}

	@Override
	public boolean isParallel() {
		return stream.isParallel();
	}

	@Override
	public Iterator iterator() {
		return stream.iterator();
	}

	@Override
	public BaseStream onClose(Runnable arg0) {
		return stream.onClose(arg0);
	}

	@Override
	public BaseStream parallel() {
		return stream.parallel();
	}

	@Override
	public BaseStream sequential() {
		return stream.sequential();
	}

	@Override
	public Spliterator spliterator() {
		return stream.spliterator();
	}

	@Override
	public BaseStream unordered() {
		return stream.unordered();
	}

  @Override
	public boolean allMatch(Predicate arg0) {
		return stream.allMatch(arg0);
	}

	@Override
	public boolean anyMatch(Predicate arg0) {
		return stream.anyMatch(arg0);
	}

	@Override
	public Object collect(Collector arg0) {
		return stream.collect(arg0);
	}

	@Override
	public Object collect(Supplier arg0, BiConsumer arg1, BiConsumer arg2) {
		return stream.collect(arg0, arg1, arg2);
	}

	@Override
	public long count() {
		return stream.count();
	}

	@Override
	public java.util.stream.Stream distinct() {
		return stream.distinct();
	}

	@Override
	public java.util.stream.Stream filter(Predicate arg0) {
		return stream.filter(arg0);
	}

	@Override
	public Optional findAny() {
		return stream.findAny();
	}

	@Override
	public Optional findFirst() {
		return stream.findFirst();
	}

	@Override
	public java.util.stream.Stream flatMap(Function arg0) {
		return stream.flatMap(arg0);
	}

	@Override
	public DoubleStream flatMapToDouble(Function arg0) {
		return stream.flatMapToDouble(arg0);
	}

	@Override
	public IntStream flatMapToInt(Function arg0) {
		return stream.flatMapToInt(arg0);
	}

	@Override
	public LongStream flatMapToLong(Function arg0) {
		return stream.flatMapToLong(arg0);
	}

	@Override
	public void forEach(Consumer arg0) {
		stream.forEach(arg0);
	}

	@Override
	public void forEachOrdered(Consumer arg0) {
		stream.forEachOrdered(arg0);
	}

	@Override
	public java.util.stream.Stream limit(long arg0) {
		return stream.limit(arg0);
	}

	@Override
	public java.util.stream.Stream map(Function arg0) {
		return stream.map(arg0);
	}

	@Override
	public DoubleStream mapToDouble(ToDoubleFunction arg0) {
		return stream.mapToDouble(arg0);
	}

	@Override
	public IntStream mapToInt(ToIntFunction arg0) {
		return stream.mapToInt(arg0);
	}

	@Override
	public LongStream mapToLong(ToLongFunction arg0) {
		return stream.mapToLong(arg0);
	}

	@Override
	public Optional max(Comparator arg0) {
		return stream.max(arg0);
	}

	@Override
	public Optional min(Comparator arg0) {
		return stream.min(arg0);
	}

	@Override
	public boolean noneMatch(Predicate arg0) {
		return stream.noneMatch(arg0);
	}

	@Override
	public java.util.stream.Stream peek(Consumer arg0) {
		return stream.peek(arg0);
	}

	@Override
	public Optional reduce(BinaryOperator arg0) {
		return stream.reduce(arg0);
	}

	@Override
	public Object reduce(Object arg0, BinaryOperator arg1) {
		return stream.reduce(arg0, arg1);
	}

	@Override
	public Object reduce(Object arg0, BiFunction arg1, BinaryOperator arg2) {
		return stream.reduce(arg0, arg1, arg2);
	}

	@Override
	public java.util.stream.Stream skip(long arg0) {
		return stream.skip(arg0);
	}

	@Override
	public java.util.stream.Stream sorted() {
		return stream.sorted();
	}

	@Override
	public java.util.stream.Stream sorted(Comparator arg0) {
		return stream.sorted(arg0);
	}

	@Override
	public Object[] toArray() {
		return stream.toArray();
	}

	@Override
	public Object[] toArray(IntFunction arg0) {
		return stream.toArray(arg0);
	}


	// Temporal Method...
	public int [] toIntArray(){

		Object [] obj_arr = stream.toArray();
		int [] int_arr = new int[obj_arr.length];

		for(int i = 0 ; i < obj_arr.length ; i++)	{
			int_arr[i] = (Integer)obj_arr[i];
		}

		return int_arr;
	}

	public List toList() {
		return (List)stream.collect( Collectors.toList() );
	}

}
