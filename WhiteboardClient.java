package app;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class WhiteboardClient extends JFrame {
    private JPanel panel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Color currentColor = Color.BLACK;
    private String currentShape = "Line";
    private int currentStroke = 1;
    private boolean eraserMode = false;

    public WhiteboardClient(String serverAddress) throws IOException {
        setTitle("Whiteboard");

        socket = new Socket(serverAddress, 8080); // Port 8080
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        //color selections
        String[] colors = {"Black", "Red", "Green", "Blue"};
        JComboBox<String> colorDropdown = new JComboBox<>(colors);
        colorDropdown.addActionListener(e -> {
            String selectedColor = (String) colorDropdown.getSelectedItem();
            switch (selectedColor) {
                case "Black":
                    currentColor = Color.BLACK;
                    break;
                case "Red":
                    currentColor = Color.RED;
                    break;
                case "Green":
                    currentColor = Color.GREEN;
                    break;
                case "Blue":
                    currentColor = Color.BLUE;
                    break;
            }
            eraserMode = false;
        });

        // special button
        JButton colorChooserButton = createButton("Choose Color");
        colorChooserButton.addActionListener(e -> {
            Color chosenColor = JColorChooser.showDialog(null, "Choose a Color", currentColor);
            if (chosenColor != null) {
                currentColor = chosenColor;
                eraserMode = false;
            }
        });

        // spinner for stroke
        JSpinner strokeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
        strokeSpinner.addChangeListener(e -> currentStroke = (int) strokeSpinner.getValue());

        // shape buttons
        JButton lineButton = createButton("Line");
        lineButton.addActionListener(e -> {
            currentShape = "Line";
            eraserMode = false;
        });

        JButton rectButton = createButton("Rectangle");
        rectButton.addActionListener(e -> {
            currentShape = "Rectangle";
            eraserMode = false;
        });

        JButton circleButton = createButton("Circle");
        circleButton.addActionListener(e -> {
            currentShape = "Circle";
            eraserMode = false;
        });

        JButton triangleButton = createButton("Triangle");
        triangleButton.addActionListener(e -> {
            currentShape = "Triangle";
            eraserMode = false;
        });

        // eraser
        JButton eraserButton = createButton("Eraser");
        eraserButton.addActionListener(e -> eraserMode = true);

        topPanel.add(new JLabel("Color:"));
        topPanel.add(colorDropdown);
        topPanel.add(colorChooserButton);
        topPanel.add(new JLabel("Stroke:"));
        topPanel.add(strokeSpinner);
        topPanel.add(lineButton);
        topPanel.add(rectButton);
        topPanel.add(circleButton);
        topPanel.add(triangleButton);
        topPanel.add(eraserButton);

        panel = new JPanel() {
            private int x, y, prevX, prevY;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        prevX = e.getX();
                        prevY = e.getY();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        x = e.getX();
                        y = e.getY();
                        Graphics g = getGraphics();
                        if (eraserMode) {
                            g.setColor(panel.getBackground());
                        } else {
                            g.setColor(currentColor);
                        }
                        ((Graphics2D) g).setStroke(new BasicStroke(currentStroke));
                        drawShape(g, prevX, prevY, x, y, currentShape);
                        out.println(currentShape + "," + g.getColor().getRGB() + "," + currentStroke + "," + prevX + "," + prevY + "," + x + "," + y);
                    }
                });

                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (currentShape.equals("Line")) {
                            x = e.getX();
                            y = e.getY();
                            Graphics g = getGraphics();
                            if (eraserMode) {
                                g.setColor(panel.getBackground());
                            } else {
                                g.setColor(currentColor);
                            }
                            ((Graphics2D) g).setStroke(new BasicStroke(currentStroke));
                            drawShape(g, prevX, prevY, x, y, currentShape);
                            out.println(currentShape + "," + g.getColor().getRGB() + "," + currentStroke + "," + prevX + "," + prevY + "," + x + "," + y);
                            prevX = x;
                            prevY = y;
                        }
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

            }
        };

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(",");
                    String shape = parts[0];
                    Color color = new Color(Integer.parseInt(parts[1]));
                    int stroke = Integer.parseInt(parts[2]);
                    int x1 = Integer.parseInt(parts[3]);
                    int y1 = Integer.parseInt(parts[4]);
                    int x2 = Integer.parseInt(parts[5]);
                    int y2 = Integer.parseInt(parts[6]);

                    Graphics g = panel.getGraphics();
                    g.setColor(color);
                    ((Graphics2D) g).setStroke(new BasicStroke(stroke));
                    drawShape(g, x1, y1, x2, y2, shape);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void drawShape(Graphics g, int x1, int y1, int x2, int y2, String shape) {
        switch (shape) {
            case "Line":
                g.drawLine(x1, y1, x2, y2);
                break;
            case "Rectangle":
                g.drawRect(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
                break;
            case "Circle":
                g.drawOval(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
                break;
            case "Triangle":
                int[] xPoints = {x1, x2, (x1 + x2) / 2};
                int[] yPoints = {y1, y1, y2};
                g.drawPolygon(xPoints, yPoints, 3);
                break;
        }
    }


    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.LIGHT_GRAY);
        button.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        button.setBorder(new EmptyBorder(5, 10, 5, 10));
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        button.setUI(new RoundedButtonUI());
        return button;
    }

    public static void main(String[] args) {
        try {
            new WhiteboardClient("52.231.107.82"); // use server IP
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class RoundedButtonUI extends BasicButtonUI {
        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c.getBackground());
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
            super.paint(g2, c);
            g2.dispose();
        }

        @Override
        protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintText(g2, c, textRect, text);
            g2.dispose();
        }
    }
}
