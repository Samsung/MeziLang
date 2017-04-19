package mezic.test.code;

public class SwitchTest {

  @SuppressWarnings("unused")
  public static void main(String[] args) {

    int a = 10;
    int nok_cnt = 0;
    nok_cnt += multi_loop(100, 100, 0, 0, false, 9901);
    nok_cnt += multi_loop(100, 100, 1, 1, false, 9902);
    nok_cnt += multi_loop(100, 100, 2, 2, false, 9903);
    nok_cnt += multi_loop(100, 100, 5, 5, false, 9906);
    nok_cnt += multi_loop(100, 100, 10, 10, false, 9911);

    // UnitTest.printf("multi for loop - break on both loop\n")
    nok_cnt += multi_loop(100, 100, 0, 0, true, 1);
    nok_cnt += multi_loop(100, 100, 1, 1, true, 102);
    nok_cnt += multi_loop(100, 100, 2, 2, true, 203);
    nok_cnt += multi_loop(100, 100, 5, 5, true, 506);
    nok_cnt += multi_loop(100, 100, 10, 10, true, 1011);
    System.out.println("Completed");
  }

  @SuppressWarnings("unused")
  public static int multi_loop(int first_loop_max, int second_loop_max, int break_i, int break_j, boolean out_break,
      int criteria) {
    int sum = 0;
    int i;
    int j;
    for (i = 0; i < first_loop_max; i++) {
      for (j = 0; j < second_loop_max; j++) {
        sum += 1;

        if (i == break_i && j == break_j) {
          break;
        }

      }

      if (out_break) {
        if (i == break_i && j == break_j) {
          break;
        }
      }

    }

    // return UnitTest.expression_test(sum, criteria, "sum = "+sum)
    return 0;
  }

}
