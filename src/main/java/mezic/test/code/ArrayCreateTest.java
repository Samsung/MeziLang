package mezic.test.code;

import java.lang.reflect.Array;

public class ArrayCreateTest {

  @SuppressWarnings({ "unused", "rawtypes" })
  public static void main(String[] args) throws ClassNotFoundException {
    /*
     * int[] a = null; a = (int[]) Array.newInstance( int.class, 10); for(int
     * i=0;i<a.length;i++) a[i] = i; for(int i=0;i<a.length;i++)
     * System.out.print(a[i]+", ");
     *
     * int[][] a2 = null; a2 = (int[][]) Array.newInstance( int.class, 10, 10);
     * for(int i=0;i< 10;i++) for(int j=0;j< 10; j++) a2[i][j] = i * j;
     *
     * for(int i=0;i< 10;i++) { for(int j=0;j< 10; j++)
     * System.out.print(a2[i][j]+", "); System.out.println(); }
     */

    // int[][] a3 = (int[][]) Array.newInstance( int.class, 10, 10);
    java.util.LinkedList[][] a3 = (java.util.LinkedList[][]) Array.newInstance(java.util.LinkedList.class, 10, 10);

    /*
     * for(int i=0;i< 10;i++) for(int j=0;j< 10; j++) for(int k=0;k< 10; k++)
     * a3[i][j][k] = i * j * k;
     *
     * for(int i=0;i< 10;i++) { for(int j=0;j< 10; j++){ for(int k=0;k< 10; k++)
     * System.out.print(a3[i][j][k]+", "); System.out.println(); }
     * System.out.println(); }
     *
     *
     * String[] str = null; str = (String[]) Array.newInstance( String.class,
     * 10); for(int i=0;i<10;i++) str[i] = Integer.toString(i); for(int
     * i=0;i<a.length;i++) System.out.print(str[i]+", ");
     */

  }

}
