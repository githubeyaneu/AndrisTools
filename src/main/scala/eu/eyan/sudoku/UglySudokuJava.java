package eu.eyan.sudoku;

public class UglySudokuJava {

  static int[][] grid =
      {{5, 3, 0, 0, 7, 0, 0, 0, 0}, {6, 0, 0, 1, 9, 5, 0, 0, 0}, {0, 9, 8, 0, 0, 0, 0, 6, 0},
          {8, 0, 0, 0, 6, 0, 0, 0, 3}, {4, 0, 0, 8, 0, 3, 0, 0, 1}, {7, 0, 0, 0, 2, 0, 0, 0, 6},
          {0, 6, 0, 0, 0, 0, 2, 8, 0}, {0, 0, 0, 4, 1, 9, 0, 0, 5}, {0, 0, 0, 0, 8, 0, 0, 7, 9}};

  static boolean possible(int x, int y, int n) {
    for (int i = 1; i < 10; i++)
      if (grid[x - 1][i - 1] == n)
        return false;
    for (int i = 1; i < 10; i++)
      if (grid[i - 1][y - 1] == n)
        return false;
    int x0 = ((x - 1) / 3) * 3;
    int y0 = ((y - 1) / 3) * 3;
    for (int i = 1; i < 4; i++)
      for (int j = 1; j < 4; j++)
        if (grid[x0 + i - 1][y0 + j - 1] == n)
          return false;

    return true;
  }

  static void solve() {
    for (int i = 1; i < 10; i++)
      for (int j = 1; j < 10; j++)
        if (grid[i - 1][j - 1] == 0) {
          for (int n = 1; n < 10; n++)
            if (possible(i, j, n)) {
              grid[i - 1][j - 1] = n;
              solve();
              grid[i - 1][j - 1] = 0;
            }
          return;
        }
    for (int i = 1; i < 10; i++)
      for (int j = 1; j < 10; j++)
        System.out.print(" " + grid[i - 1][j - 1] + (j % 9 == 0 ? "\r\n" : ""));
  }

  public static void main(String[] args) {
    long st = System.currentTimeMillis();
    solve();
    System.out.println(System.currentTimeMillis() - st + "ms");
  }
}
