package com.fixpoint.business.attachments.service;

import com.fixpoint.business.attachments.entity.Attachment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AttachmentThumbnailService {

    private static final int THUMBNAIL_WIDTH = 288;
    private static final int THUMBNAIL_HEIGHT = 180;
    private static final int CORNER_RADIUS = 18;

    private final FileStorageService fileStorageService;

    public Resource loadOrCreateThumbnail(Attachment attachment) {
        try {
            Path thumbnailPath = fileStorageService.resolveThumbnailPath(attachment.getFilepath());
            if (Files.notExists(thumbnailPath)) {
                createThumbnail(attachment);
            }
            return fileStorageService.loadThumbnailAsResource(attachment.getFilepath());
        } catch (RuntimeException ex) {
            throw new EntityNotFoundException("Thumbnail not available for attachment " + attachment.getId());
        }
    }

    public void createThumbnail(Attachment attachment) {
        BufferedImage thumbnailImage = buildThumbnail(attachment);
        Path thumbnailPath = fileStorageService.resolveThumbnailPath(attachment.getFilepath());
        try {
            Files.createDirectories(thumbnailPath.getParent());
            ImageIO.write(thumbnailImage, "png", thumbnailPath.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create thumbnail for attachment " + attachment.getId(), ex);
        }
    }

    public void deleteThumbnail(String storedFileName) {
        fileStorageService.deleteThumbnail(storedFileName);
    }

    private BufferedImage buildThumbnail(Attachment attachment) {
        BufferedImage sourceImage = loadSourceImage(attachment);
        if (sourceImage != null) {
            return buildImageThumbnail(sourceImage, attachment);
        }
        return buildPlaceholderThumbnail(attachment);
    }

    private BufferedImage loadSourceImage(Attachment attachment) {
        if (!"image".equalsIgnoreCase(attachment.getFileType())) {
            return null;
        }

        try {
            Path originalFile = fileStorageService.resolveStoredFilePath(attachment.getFilepath());
            if (Files.notExists(originalFile)) {
                return null;
            }
            return ImageIO.read(originalFile.toFile());
        } catch (IOException ex) {
            return null;
        }
    }

    private BufferedImage buildImageThumbnail(BufferedImage sourceImage, Attachment attachment) {
        BufferedImage canvas = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();

        applyQualityHints(graphics);
        graphics.setClip(new RoundRectangle2D.Float(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS));
        paintScaledImage(graphics, sourceImage);
        paintFooterOverlay(graphics, attachment, new Color(15, 28, 36, 185), new Color(255, 255, 255, 230));

        graphics.dispose();
        return canvas;
    }

    private BufferedImage buildPlaceholderThumbnail(Attachment attachment) {
        BufferedImage canvas = new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();

        applyQualityHints(graphics);
        graphics.setClip(new RoundRectangle2D.Float(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS));

        Color primary = resolvePrimaryColor(attachment.getFileType());
        Color secondary = primary.brighter();
        GradientPaint gradient = new GradientPaint(0, 0, secondary, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, primary);
        graphics.setPaint(gradient);
        graphics.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

        graphics.setColor(new Color(255, 255, 255, 46));
        graphics.fillRoundRect(18, 18, THUMBNAIL_WIDTH - 36, THUMBNAIL_HEIGHT - 36, 24, 24);

        String extension = attachment.getFileFormat() == null
                ? attachment.getFileType()
                : attachment.getFileFormat().toUpperCase(Locale.ROOT);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        drawCenteredText(graphics, extension, 70);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        drawCenteredText(graphics, compactFilename(attachment.getFilename(), 22), 112);

        paintFooterOverlay(graphics, attachment, new Color(18, 32, 42, 148), new Color(255, 255, 255, 230));

        graphics.dispose();
        return canvas;
    }

    private void paintScaledImage(Graphics2D graphics, BufferedImage sourceImage) {
        double scale = Math.max(
                (double) THUMBNAIL_WIDTH / Math.max(1, sourceImage.getWidth()),
                (double) THUMBNAIL_HEIGHT / Math.max(1, sourceImage.getHeight())
        );

        int targetWidth = (int) Math.round(sourceImage.getWidth() * scale);
        int targetHeight = (int) Math.round(sourceImage.getHeight() * scale);
        int offsetX = (THUMBNAIL_WIDTH - targetWidth) / 2;
        int offsetY = (THUMBNAIL_HEIGHT - targetHeight) / 2;

        graphics.drawImage(sourceImage, offsetX, offsetY, targetWidth, targetHeight, null);
    }

    private void paintFooterOverlay(Graphics2D graphics, Attachment attachment, Color overlayColor, Color textColor) {
        graphics.setColor(overlayColor);
        graphics.fillRect(0, THUMBNAIL_HEIGHT - 42, THUMBNAIL_WIDTH, 42);

        graphics.setColor(textColor);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        graphics.drawString(compactFilename(attachment.getFilename(), 26), 16, THUMBNAIL_HEIGHT - 17);

        if (attachment.getTag() != null && !attachment.getTag().isBlank()) {
            String tag = compactFilename(attachment.getTag(), 16);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            int badgeWidth = Math.min(110, graphics.getFontMetrics().stringWidth(tag) + 18);
            int badgeX = THUMBNAIL_WIDTH - badgeWidth - 14;
            int badgeY = THUMBNAIL_HEIGHT - 33;
            graphics.setColor(new Color(255, 255, 255, 48));
            graphics.fillRoundRect(badgeX, badgeY, badgeWidth, 22, 12, 12);
            graphics.setColor(Color.WHITE);
            graphics.drawString(tag, badgeX + 9, badgeY + 15);
        }
    }

    private void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private void drawCenteredText(Graphics2D graphics, String text, int baselineY) {
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (THUMBNAIL_WIDTH - metrics.stringWidth(text)) / 2;
        graphics.drawString(text, Math.max(16, x), baselineY);
    }

    private Color resolvePrimaryColor(String fileType) {
        return switch (fileType == null ? "" : fileType.toLowerCase(Locale.ROOT)) {
            case "image" -> new Color(32, 112, 154);
            case "document" -> new Color(58, 110, 83);
            case "spreadsheet" -> new Color(96, 122, 46);
            case "archive" -> new Color(114, 78, 145);
            default -> new Color(81, 92, 108);
        };
    }

    private String compactFilename(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
