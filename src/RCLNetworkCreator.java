import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;

public class RCLNetworkCreator extends JPanel {
    private static int frameWidth = 1000;
    private static int frameHeight = 1000;
    //network parameters
    private static int resistors = 7;
    private static int capacitors = 1;
    private static int inductors = 1;
    private static int minPartValue = 5;
    private static int maxPartValue = 20;
    private static int minVoltage = 1;
    private static int maxVoltage = 10;
    private static int maxComponentsPerCircuit = (resistors + capacitors + inductors) / 3;
    private static int maxUselessResistors = 3; //how many resistors are allowed to be shorted etc.
    private static String tConf = "both"; //evaluation time
    //visual parameters
    private static int cWidth = 25;
    private static int cHeight = 50;
    private static int lineLength = 15;

    public static void main(String[] args) {
        if (!readArgs(args))
            return;
        //read config (just saves number of generated network for numbering)
        String conf = readConfig();

        //build network
        Network n;
        do //loop until output satisfies all quality control conditions
        {
            n = generateRandomNetwork(resistors, capacitors, inductors);
            n = simplifyNetwork(n);
        } while (n.getMaxComponentsPerCircuit() > maxComponentsPerCircuit || getNumOfUselessResistors(n) > maxUselessResistors);

        //save network
        boolean t0 = tConf.equals("t0");
        saveValuesToFile(n, conf + " Task", true, false);
        if (tConf.equals("both"))
        {
            saveValuesToFile(n, conf + " t0 Solution", true, true);
            saveValuesToFile(n, conf + " tInf Solution", false, true);
        }
        else
            saveValuesToFile(n, conf + " " + tConf + " Solution", t0, true);
        //show network
        /*
        JFrame frame = new JFrame();
        frame.setSize(frameWidth, frameHeight);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(n);
        frame.setVisible(true);
        */

        saveNetworkToFile(n, conf + " Image");
    }

    public static boolean readArgs(String[] args) {
        try
        {
            HashMap<String, String> map = new HashMap<>();
            for (int i = 0; i < args.length; i += 2)
                map.put(args[i], args[i + 1]);

            if (map.containsKey("-nRes"))
                resistors = Integer.parseInt(map.remove("-nRes"));
            if (map.containsKey("-nCap"))
                capacitors = Integer.parseInt(map.remove("-nCap"));
            if (map.containsKey("-nInd"))
                inductors = Integer.parseInt(map.remove("-nInd"));
            if (map.containsKey("-minPart"))
                minPartValue = Integer.parseInt(map.remove("-minPart"));
            if (map.containsKey("-maxPart"))
                maxPartValue = Integer.parseInt(map.remove("-maxPart"));
            if (map.containsKey("-minV"))
                minVoltage = Integer.parseInt(map.remove("-minV"));
            if (map.containsKey("-maxV"))
                maxVoltage = Integer.parseInt(map.remove("-maxV"));
            if (map.containsKey("-maxComp"))
                maxComponentsPerCircuit = Integer.parseInt(map.remove("-maxComp"));
            if (map.containsKey("-maxUseless"))
                maxUselessResistors = Integer.parseInt(map.remove("-maxUseless"));
            if (map.containsKey("-cWidth"))
                cWidth = Integer.parseInt(map.remove("-cWidth"));
            if (map.containsKey("-cHeight"))
                cHeight = Integer.parseInt(map.remove("-cHeight"));
            if (map.containsKey("-lines"))
                lineLength = Integer.parseInt(map.remove("-lines"));
            if (map.containsKey("-time"))
                tConf = map.remove("-time");
            if (!map.isEmpty() || !(tConf.equals("t0") || tConf.equals("tInf") || tConf.equals("both")))
                throw new IllegalArgumentException();
        } catch (Exception e)
        {
            System.err.println("""
                    Wrong arguments, options are:
                    -nRes: number of resistors
                    -nCap: number of capacitors
                    -nInd: number of inductors
                    -minPart: minimum resistance value
                    -maxPart: maximum resistance value
                    -minV: minimum voltage
                    -maxV: maximum voltage
                    -maxComp: maximum components per circuit
                    -maxUseless: maximum shorted resistors
                    -cWidth: width of component
                    -cHeight: height of component
                    -lines: length of vertical connections
                    -time: "t0"=evaluate at t0, "tInf": evaluate at settled state, default=do both
                    """
            );
            return false;
        }
        return true;
    }

    static String readConfig() {
        String data = "";
        try
        {
            File f = new File("config");
            if (f.isFile())
            {
                Scanner myReader = new Scanner(f);
                data = myReader.nextLine();
                int num = Integer.parseInt(data);
                num++;
                FileWriter writer = new FileWriter("config");
                writer.write(num + "");
                writer.close();
                data = num + "";
                myReader.close();
            }
            else //no config, create
            {
                FileWriter writer = new FileWriter("config");
                writer.write("1");
                data = "1";
                writer.close();
            }

            return data;
        } catch (Exception e)
        {
            return "default";//program should at least work without saving old networks
        }
    }

    public static Network generateRandomNetwork(int numberR, int numberC, int numberI) {
        ArrayList<Network> components = new ArrayList<>();
        for (int i = 0; i < numberR; i++)
            components.add(new Resistor("R" + i, (int) (Math.random() * (maxPartValue - minPartValue)) + minPartValue));
        for (int i = 0; i < numberC; i++)
            components.add(new Capacitor("C" + i, (int) (Math.random() * (maxPartValue - minPartValue)) + minPartValue));
        for (int i = 0; i < numberI; i++)
            components.add(new Inductor("L" + i, (int) (Math.random() * (maxPartValue - minPartValue)) + minPartValue));

        boolean parallel = !(Math.random() > 0.5); //what is the highest network type
        while (components.size() > 1)
        {
            //select type
            Circuit nt;
            if (parallel)
                nt = new Parallel(null);
            else
                nt = new Serial(null);
            //select components
            int numComp = 2 + (int) (Math.random() * (components.size() - 2)) % maxComponentsPerCircuit;
            for (int i = 0; i < numComp; i++)
            {
                int c = (int) (Math.random() * components.size());
                //if (nt.getMaxComponentsPerCircuit() < maxComponentsPerCircuit || !components.get(c).isComponent())
                nt.add(components.remove(c)); //todo make performant
                //else i--;
            }
            components.add(nt);
            parallel = !parallel;
        }
        return components.get(0);
    }


    /**
     * resolves nesting of nonComponents of the same Type, Parallel(Parallel(R1,R2),R2) -> Parallel(R1,R2,R3)
     */
    public static Network simplifyNetwork(Network network) {
        if (Component.class.isAssignableFrom(network.getClass())) //is component
            return network;

        Circuit nt = (Circuit) network;
        ArrayList<Network> toAdd = new ArrayList<>();
        ArrayList<Network> toRemove = new ArrayList<>();
        for (Network n : nt.getContent()) //simplify lower levels
        {
            n = simplifyNetwork(n); //simplify recursively
            if (nt.getClass() == n.getClass())
            {
                toAdd.addAll(((Circuit) n).getContent());
                toRemove.add(n);
            }
        }
        nt.removeAll(toRemove);
        nt.addAll(toAdd);
        return nt;
    }

    public static int getNumOfUselessResistors(Network network) {
        HashMap<String, ArrayList<Double>> values = network.getValuesForParts(true, 5);
        int res = 0;
        for (String key : values.keySet())
            if (key.charAt(0) == 'R') //is resistor
                if (values.get(key).get(1) == 0.0) //has no voltage
                    res++;
        return res;
    }

    public static void saveNetworkToFile(Network n, String filename) {
        BufferedImage bi = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB); //TODO ugly
        n.paintComponent(bi.getGraphics());
        bi = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB);
        n.paintComponent(bi.getGraphics());

        File output = new File(filename + ".png");
        try
        {
            ImageIO.write(bi, "png", output);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void saveValuesToFile(Network n, String filename, boolean t0, boolean solution) {
        int voltage = (int) (Math.random() * (maxVoltage - minVoltage) + minVoltage);
        double resistance = Math.round(n.getResistance(t0) * 1000) / 1000.0;
        double current = Math.round(voltage / resistance * 1000) / 1000.0;

        try
        {
            FileWriter writer = new FileWriter(filename + ".txt");
            writer.write(n + "\n\n");
            if (solution)
                writer.write(resistance + " Ω  " + voltage + "V  " + current + "A\ncomponent: [Resistance, Voltage, Current]\n");
            else
                writer.write(voltage + "V\n");
            HashMap<String, ArrayList<Double>> values = n.getValuesForParts(t0, voltage);
            for (String key : values.keySet())
            {
                ArrayList<Double> al = values.get(key);
                for (int i = 0; i < al.size(); i++)
                {
                    double val = al.get(i);
                    if (!Double.isInfinite(val) && val != 0.0 && !Double.isNaN(val))
                        al.set(i, Math.round(val * 1000) / 1000.0);
                }

                if (solution)
                    writer.write(key + ": " + values.get(key) + "\n");
                else if (key.charAt(0) == 'R') //only resistors
                    writer.write(key + ": " + values.get(key).get(0) + " Ω\n");
            }
            writer.close();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    abstract static class Network extends JPanel {
        /**
         * returns ArrayList for each Component with AL[0] = Resistance,  AL[1] = voltage, AL[2] = Current (before part)
         *
         * @param t0 if true evaluate network immediately after voltage is applied, else evaluate in settled state
         */
        public abstract HashMap<String, ArrayList<Double>> getValuesForParts(boolean t0, double voltage);

        public abstract double getResistance(boolean t0);

        public void paintComponent(Graphics g) {
            int[] res = draw(g, 100, 50);

            g.drawLine(50, 50, 50, 100); //vertical1
            g.drawLine(50, 50, 100 + cWidth / 2, 50); //horizontal1
            g.drawLine(50, 100, 50, res[1] + lineLength); //vertical2
            g.drawLine(50, res[1] + lineLength, 100 + cWidth / 2, res[1] + lineLength); //horizontal2
            g.drawLine(100 + cWidth / 2, res[1], 100 + cWidth / 2, res[1] + lineLength); //vertical3
            frameWidth = res[0] + 20;
            frameHeight = res[1] + lineLength + 20;
            //voltage source
            g.drawOval(25, 100, 50, 50);
            //arrow
            g.drawLine(15, 100, 15, 150);
            g.drawLine(10, 140, 15, 150);
            g.drawLine(20, 140, 15, 150);
        }

        /**
         * draws network and marks used space by returning the maximum x and y coordinates occupied
         * by the image in int[0] and int[1], returns the x value for the bottom connection in int[2]
         */
        public abstract int[] draw(Graphics g, int x, int y);

        public abstract int getMaxComponentsPerCircuit();

        public abstract boolean isComponent();
    }

    abstract static class Circuit extends Network {
        ArrayList<Network> parts;

        public ArrayList<Network> getContent() {
            return parts;
        }

        public void add(Network network) {
            parts.add(network);
        }

        public void addAll(ArrayList<Network> networks) {
            parts.addAll(networks);
        }

        public void removeAll(Collection<Network> networks) {
            parts.removeAll(networks);
        }

        public int getMaxComponentsPerCircuit() {
            int res = 0;
            int max = 0;
            for (Network part : parts)
            {
                if (part.isComponent())
                    res++;
                else
                    max = Math.max(max, part.getMaxComponentsPerCircuit());
            }
            return Math.max(max, res);
        }

        public boolean isComponent() {
            return false;
        }
    }

    static class Serial extends Circuit {
        public Serial(ArrayList<Network> parts) {
            if (parts == null)
                this.parts = new ArrayList<>();
            else
                this.parts = parts;
        }

        public HashMap<String, ArrayList<Double>> getValuesForParts(boolean t0, double voltage) {
            HashMap<String, ArrayList<Double>> res = new HashMap<>();
            //count how many parts have infinite resistance. If 2 or more do, none of them have voltage
            int inf = 0;
            for (Network part : parts)
                if (Double.isInfinite(part.getResistance(t0)))
                    inf++;

            for (Network part : parts)
            {
                double partRes = part.getResistance(t0);
                double partVoltage = (voltage * partRes) / (getResistance(t0));
                if (Double.isInfinite(partRes) && inf > 1)
                    partVoltage = 0.0;
                else if (Double.isInfinite(partRes) && Double.isInfinite(getResistance(t0))) //single inf part gets all voltage
                    partVoltage = voltage;
                res.putAll(part.getValuesForParts(t0, partVoltage));
            }
            return res;
        }

        public double getResistance(boolean t0) {
            double res = 0;
            for (Network part : parts) res += part.getResistance(t0);
            return res;
        }

        public int[] draw(Graphics g, int x, int y) {
            //saves the maximum coordinates parts of the serial used while printing to tell caller how big the
            //complete drawing got
            int maxX = x;
            int maxY = y;
            for (Network part : parts)
            {
                int[] res = part.draw(g, x, y);
                y = res[1];
                maxX = Math.max(res[0], maxX);
                maxY = Math.max(res[1], maxY);
            }
            return new int[]{maxX, maxY, x + cWidth / 2};
        }

        public String toString() {
            String res = "Serial(";
            for (Network part : parts) res += part.toString() + ",";
            return res.substring(0, res.length() - 1) + ")";
        }
    }

    static class Parallel extends Circuit {

        public Parallel(ArrayList<Network> parts) {
            if (parts == null)
                this.parts = new ArrayList<>();
            else
                this.parts = parts;
        }

        public HashMap<String, ArrayList<Double>> getValuesForParts(boolean t0, double voltage) {
            HashMap<String, ArrayList<Double>> res = new HashMap<>();
            if (getResistance(t0) == 0.0) //check if shorted
                voltage = 0.0;
            for (Network part : parts) res.putAll(part.getValuesForParts(t0, voltage));
            return res;
        }

        public double getResistance(boolean t0) {
            ArrayList<Double> resistances = new ArrayList<>();
            for (Network part : parts) resistances.add(part.getResistance(t0));

            if (resistances.contains(0.0)) //shorted
                return 0.0;
            while (resistances.remove(Double.POSITIVE_INFINITY)) ;
            if (resistances.isEmpty())
                return Double.POSITIVE_INFINITY;

            double res = resistances.get(0);
            for (int i = 1; i < resistances.size(); i++)
            {
                double resistance = resistances.get(i);
                res = (res * resistance) / (res + resistance);
            }
            return res;
        }

        public String toString() {
            String res = "Parallel(";
            for (Network part : parts) res += part.toString() + ",";
            return res.substring(0, res.length() - 1) + ")";
        }

        public int[] draw(Graphics g, int x, int y) {
            int maxX = x;
            int maxY = y;
            int startX = x + cWidth / 2;
            int[] endY = new int[parts.size()]; //save all y end coords for bottom connection
            int[] connectionX = new int[parts.size()];

            g.drawLine(startX, y, startX, y + lineLength); //vertical connection  for whole parallel
            y += lineLength;
            for (int i = 0; i < parts.size(); i++)
            {
                int[] res = parts.get(i).draw(g, x, y);
                endY[i] = res[1];
                connectionX[i] = res[2];
                x = res[0] + 30; //30 space
                maxX = Math.max(res[0], maxX);
                maxY = Math.max(res[1], maxY);
            }
            g.drawLine(startX, y, connectionX[connectionX.length - 1], y);//horizontal top connection
            maxY += lineLength; //for bottom line

            for (int i = 0; i < endY.length; i++) //draw bottom connections
                g.drawLine(connectionX[i], endY[i], connectionX[i], maxY);

            g.drawLine(startX, maxY, connectionX[connectionX.length - 1], maxY);//horizontal bottom connection

            return new int[]{maxX, maxY, startX + cWidth / 2};
        }
    }


    abstract static class Component extends Network {
        String name;

        public HashMap<String, ArrayList<Double>> getValuesForParts(boolean t0, double voltage) {
            HashMap<String, ArrayList<Double>> res = new HashMap<>();
            ArrayList<Double> al = new ArrayList<>();
            double R = getResistance(t0);
            al.add(0, R);
            if (R == 0.0)
            {
                al.add(1, 0.0);
                al.add(2, Double.POSITIVE_INFINITY);
            }
            else if (Double.isInfinite(R))
            {
                al.add(1, voltage);
                al.add(2, 0.0);
            }
            else
            {
                al.add(1, voltage);
                al.add(2, voltage / R);
            }

            res.put(name, al);
            return res;
        }

        public int[] draw(Graphics g, int x, int y) {
            //g.drawLine(x, y, x + cWidth, y + cHeight);
            g.drawLine(x + cWidth / 2, y, x + cWidth / 2, y + lineLength);
            g.drawRect(x, y + lineLength, cWidth, cHeight); //50 100
            g.drawString(name, x + cWidth / 4, y + lineLength + cHeight / 2);

            return new int[]{x + cWidth, y + lineLength + cHeight, x + cWidth / 2};
        }

        public int getMaxComponentsPerCircuit() {
            return 1;
        }

        public boolean isComponent() {
            return true;
        }
    }

    static class Resistor extends Component {
        double R; //in Ohm

        public Resistor(String name, double R) {
            this.name = name;
            this.R = R;
        }

        public double getResistance(boolean t0) {
            return R;
        }

        public String toString() {
            return "R";
        }
    }

    static class Capacitor extends Component {
        double C; //in F

        public Capacitor(String name, double C) {
            this.name = name;
            this.C = C;
        }

        public double getResistance(boolean t0) {
            return t0 ? 0 : Double.POSITIVE_INFINITY;
        }

        public String toString() {
            return "C";
        }
    }

    static class Inductor extends Component {
        double L;

        public Inductor(String name, double L) {
            this.name = name;
            this.L = L;
        }

        public double getResistance(boolean t0) {
            return t0 ? Double.POSITIVE_INFINITY : 0;
        }

        public String toString() {
            return "L";
        }
    }
}
