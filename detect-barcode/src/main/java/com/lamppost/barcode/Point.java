package com.lamppost.barcode;

public class Point
{
    private double x;
    private double y;

    public Point(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public String toString()
    {
        return "(" + this.x + "," + this.y + ")";
    };

    public static void main(String[] args)
    {
        Point point = new Point(3, 2);
        System.out.println(point.toString());
    };
}