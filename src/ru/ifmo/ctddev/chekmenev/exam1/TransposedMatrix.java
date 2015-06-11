package ru.ifmo.ctddev.chekmenev.exam1;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by creed on 11.06.15.
 */

public class TransposedMatrix {
    public static final int WIDTH = 500;
    public static final int HEIGHT = 500;

    public static class MatrixTableModel extends AbstractTableModel {
        private Object[][] data;
        private boolean isTransposed;
        private MatrixTableModel other;

        MatrixTableModel(Object[][] data, boolean isTransposed, MatrixTableModel other) {
            this.data = data;
            this.isTransposed = isTransposed;
            if (other != null) {
                this.other = other;
            }
        }

        /*public void setOtherModel(MatrixTableModel other) {
            this.other = other;
        }*/

        @Override
        public int getRowCount() {
            if (!isTransposed) {
                return data.length;
            } else {
                return (data.length > 0 ? data[0].length : 0);
            }
        }

        @Override
        public int getColumnCount() {
            if (isTransposed) {
                return data.length;
            } else {
                return (data.length > 0 ? data[0].length : 0);
            }
        }

        @Override
        public String getColumnName(int columnIndex) {
            return (new Integer(columnIndex)).toString();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Integer.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (isTransposed) {
                return data[columnIndex][rowIndex];
            } else {
                return data[rowIndex][columnIndex];
            }

        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (isTransposed) {
                System.out.println("set value " + rowIndex + "," + columnIndex + " = " + aValue);
                data[columnIndex][rowIndex] = aValue;
                fireTableCellUpdated(rowIndex, columnIndex);
                if (other != null) other.fireTableCellUpdated(columnIndex, rowIndex);
            } else {
                data[rowIndex][columnIndex] = aValue;
                fireTableCellUpdated(rowIndex, columnIndex);
                if (other != null) other.fireTableCellUpdated(columnIndex, rowIndex);
            }
        }
    }

    public static void createTable() {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get("matrix.txt")));
        } catch (IOException e) {

        }

        System.out.println(content);

        JFrame frame = new JFrame("Transposed Matrix");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        Object[][] data = {
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
                {new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)},
        };

        MatrixTableModel matrix = new MatrixTableModel(data, false, null);
        MatrixTableModel transposedMatrix = new MatrixTableModel(data, true, matrix);
        matrix.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                TableModel model = (TableModel) e.getSource();
                Object data1 = model.getValueAt(row, column);
                System.out.println("data[" + row + "][" + column + "] = " + data1);
                transposedMatrix.fireTableCellUpdated(column, row);
            }
        });
        transposedMatrix.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                TableModel model = (TableModel) e.getSource();
                Object data1 = model.getValueAt(row, column);
                System.out.println("transposed_data[" + row + "][" + column + "] = " + data1);
            }
        });

        JTable table1 = new JTable(matrix);
        JTable table2 = new JTable(transposedMatrix);
        table1.setTableHeader(null);
        table2.setTableHeader(null);

        JScrollPane scrollPane1 = new JScrollPane(table1);
        JScrollPane scrollPane2 = new JScrollPane(table2);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane1, scrollPane2);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(WIDTH);

        frame.getContentPane().add(splitPane);
        frame.setPreferredSize(new Dimension(2 * WIDTH, HEIGHT));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        /*(new Thread(() -> {

            try {
                Thread.sleep(2*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            matrix.setValueAt(1000, 0, 1);

            try {
                Thread.sleep(2*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            transposedMatrix.setValueAt(1000, 2, 3);

        })).start();*/
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            //JFrame.setDefaultLookAndFeelDecorated(false);
            JFrame.setDefaultLookAndFeelDecorated(true);
            createTable();
        });
    }
}
