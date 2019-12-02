
public class Progress
{
    private static int maxValue;
    private static int value;
    
    public static void StartProgress(int newMax)
    {
        value = 0;
        maxValue = newMax;
        System.err.println("\n[----+----+----+----]");
        System.err.print("[");
    }
    
    public static void SetValue(int newValue)
    {
        int oldValue = value;
        int oldPercent = value*100/maxValue;
        value = newValue;
        int newPercent = value*100/maxValue;
        if ((value == maxValue) && (oldValue < value)) System.err.println("]\n");
        else {
            while ((oldPercent/5) < (newPercent/5))
            {
                System.err.print("X");
                oldPercent += 5;
            }
        }
    }
}
