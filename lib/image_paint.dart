import 'dart:ui';

import 'package:flutter/material.dart';

class ImagePainter extends CustomPainter {
  final List<Offset> points;
  final List<List<Offset>> lines;

  ImagePainter({
    required this.points,
    required this.lines,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paintLines = Paint()
      ..color = const Color.fromARGB(255, 224, 34, 9)
      ..strokeWidth = 8.0;

    for (var line in lines) {
      canvas.drawLine(line[0], line[1], paintLines);
    }

    final paintPoints = Paint()
      ..color = const Color.fromARGB(255, 10, 224, 10)
      ..strokeWidth = 10.0
      ..style = PaintingStyle.fill;

    canvas.drawPoints(PointMode.points, points, paintPoints);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) {
    return true;
  }
}
