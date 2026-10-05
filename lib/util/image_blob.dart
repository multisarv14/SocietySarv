// Port of util/ImageBlob.kt.
//
// Public API:
//   class ImageBlob
//     static const int maxBlobChars                         // 700000
//     static const int maxEdgePx                            // 1024
//     static Future<String> fromBytesCompressed(Uint8List bytes, {int quality = 80})
//     static String fromBytes(Uint8List bytes)
//     static Uint8List? toBytes(String? base64)
//   ImageProvider? blobImageProvider(String? base64)        // cached MemoryImage, null when empty/corrupt
//   class BlobImage extends StatefulWidget
//     BlobImage({Key? key, required String? base64, double? width, double? height,
//                BoxFit fit = BoxFit.cover, Widget? placeholder})
//   Future<String?> pickImageAsBase64(BuildContext context, {ImageSource? source})
//   Future<({String base64, String name, int size})?> pickFileAsBase64({List<String>? allowedExtensions})
//
// Images are stored inside the Firestore JSON documents themselves, as Base64
// blob strings (fields ending in `Base64`), and decoded back for display. That
// keeps a document self-contained - no Cloud Storage bucket and no second round
// trip to render a photo.
//
// Firestore caps a single document at 1 MB, so [ImageBlob.fromBytesCompressed]
// compresses down to a sane size before encoding. Blob fields are also excluded
// from indexing (see `firestore.indexes.json` -> fieldOverrides).

import 'dart:convert';
import 'dart:convert' as convert show base64;
import 'dart:isolate';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:image/image.dart' as img;
import 'package:image_picker/image_picker.dart';

export 'package:image_picker/image_picker.dart' show ImageSource;

class ImageBlob {
  ImageBlob._();

  /// Largest edge, in pixels, an image is scaled down to before encoding.
  static const int maxEdgePx = 1024;

  /// Roughly 700 KB of Base64 text, comfortably inside the 1 MB doc limit.
  static const int maxBlobChars = 700000;

  /// Decodes any supported image (JPG, PNG, WebP, ...), scales it down and
  /// encodes it as the Base64 JPEG string stored in Firestore.
  ///
  /// Quality is lowered in steps of 15 until the text fits [maxBlobChars] or
  /// quality reaches 30. Throws [FormatException] when the bytes are not an
  /// image this decoder understands.
  static Future<String> fromBytesCompressed(Uint8List bytes, {int quality = 80}) {
    return Isolate.run(() => _compress(bytes, quality));
  }

  static String _compress(Uint8List bytes, int quality) {
    final decoded = img.decodeImage(bytes);
    if (decoded == null) throw const FormatException('unsupported image');
    final scaled = _downscale(decoded);
    var q = quality;
    while (true) {
      final jpg = img.encodeJpg(scaled, quality: q.clamp(1, 100));
      final encoded = base64Encode(jpg);
      if (encoded.length <= maxBlobChars || q <= 30) return encoded;
      q -= 15;
    }
  }

  static img.Image _downscale(img.Image image) {
    final longest = image.width > image.height ? image.width : image.height;
    if (longest <= maxEdgePx) return image;
    final ratio = maxEdgePx / longest;
    final w = (image.width * ratio).toInt();
    final h = (image.height * ratio).toInt();
    return img.copyResize(
      image,
      width: w < 1 ? 1 : w,
      height: h < 1 ? 1 : h,
      interpolation: img.Interpolation.linear,
    );
  }

  /// Encodes raw bytes (for example a picked file) without re-compressing.
  static String fromBytes(Uint8List bytes) => base64Encode(bytes);

  /// Decodes a stored blob back into bytes, or null when empty/corrupt.
  static Uint8List? toBytes(String? base64) {
    if (base64 == null || base64.trim().isEmpty) return null;
    try {
      final idx = base64.indexOf('base64,');
      final clean = (idx >= 0 ? base64.substring(idx + 7) : base64).trim();
      // Android's Base64.DEFAULT tolerates line breaks and missing padding.
      final bytes = base64Decode(convert.base64.normalize(clean.replaceAll(RegExp(r'\s'), '')));
      return bytes.isEmpty ? null : bytes;
    } catch (_) {
      return null;
    }
  }
}

// A small cache so the same blob string is only decoded once, wherever it is
// shown (top bar avatar, profile screen, receipt dialog...).
final Map<String, MemoryImage> _providerCache = <String, MemoryImage>{};
const int _providerCacheSize = 48;

/// A cached [MemoryImage] for a stored blob, or null when empty/corrupt.
ImageProvider? blobImageProvider(String? base64) {
  if (base64 == null || base64.trim().isEmpty) return null;
  final hit = _providerCache.remove(base64);
  if (hit != null) {
    _providerCache[base64] = hit; // most recently used goes last
    return hit;
  }
  final bytes = ImageBlob.toBytes(base64);
  if (bytes == null) return null;
  final provider = MemoryImage(bytes);
  _providerCache[base64] = provider;
  if (_providerCache.length > _providerCacheSize) {
    _providerCache.remove(_providerCache.keys.first);
  }
  return provider;
}

/// Shows a Base64 blob as an image, decoding only when the string changes
/// (Kotlin's `rememberBlobImage`). Shows [placeholder] (or nothing) when the
/// blob is empty or cannot be decoded.
class BlobImage extends StatefulWidget {
  const BlobImage({
    super.key,
    required this.base64,
    this.width,
    this.height,
    this.fit = BoxFit.cover,
    this.placeholder,
  });

  final String? base64;
  final double? width;
  final double? height;
  final BoxFit fit;
  final Widget? placeholder;

  @override
  State<BlobImage> createState() => _BlobImageState();
}

class _BlobImageState extends State<BlobImage> {
  ImageProvider? _provider;
  String? _source;

  @override
  void initState() {
    super.initState();
    _resolve();
  }

  @override
  void didUpdateWidget(covariant BlobImage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(widget.base64, _source) && widget.base64 != _source) _resolve();
  }

  void _resolve() {
    _source = widget.base64;
    _provider = blobImageProvider(widget.base64);
  }

  @override
  Widget build(BuildContext context) {
    final provider = _provider;
    if (provider == null) {
      return widget.placeholder ?? SizedBox(width: widget.width, height: widget.height);
    }
    return Image(
      image: provider,
      width: widget.width,
      height: widget.height,
      fit: widget.fit,
      gaplessPlayback: true,
      errorBuilder: (_, _, _) =>
          widget.placeholder ?? SizedBox(width: widget.width, height: widget.height),
    );
  }
}

/// Lets the user pick a photo and returns it as a compressed Base64 JPEG blob.
///
/// When [source] is null a small bottom sheet asks Camera or Gallery first.
/// Returns null when the user cancels. Throws when the picked file is not a
/// readable image - callers show "Could not read that image. Pick a JPG or PNG."
Future<String?> pickImageAsBase64(BuildContext context, {ImageSource? source}) async {
  var chosen = source;
  if (chosen == null) {
    chosen = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_camera),
              title: const Text('Camera'),
              onTap: () => Navigator.of(sheetContext).pop(ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Gallery'),
              onTap: () => Navigator.of(sheetContext).pop(ImageSource.gallery),
            ),
          ],
        ),
      ),
    );
    if (chosen == null) return null;
  }
  final picked = await ImagePicker().pickImage(source: chosen);
  if (picked == null) return null;
  final bytes = await picked.readAsBytes();
  return ImageBlob.fromBytesCompressed(bytes);
}

/// Lets the user pick a document (e.g. a rent agreement PDF) and returns its
/// raw bytes as Base64, uncompressed, with its name and size in bytes so the
/// caller can enforce limits such as RENT_AGREEMENT_MAX_BYTES.
///
/// Returns null when the user cancels; throws when the file cannot be read.
Future<({String base64, String name, int size})?> pickFileAsBase64({
  List<String>? allowedExtensions,
}) async {
  final hasFilter = allowedExtensions != null && allowedExtensions.isNotEmpty;
  final file = await FilePicker.pickFile(
    type: hasFilter ? FileType.custom : FileType.any,
    allowedExtensions: hasFilter ? allowedExtensions : null,
  );
  if (file == null) return null;
  final bytes = await file.readAsBytes();
  return (base64: ImageBlob.fromBytes(bytes), name: file.name, size: bytes.length);
}
