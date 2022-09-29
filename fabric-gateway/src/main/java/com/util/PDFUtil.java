package com.util;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.font.FontProvider;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.stereotype.Component;


import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成PDF文件
 */
@Component
public class PDFUtil {

    /**
     * 填充html模板
     *  @param templateFile 模板文件名
     * @param args         模板参数
     * @param pdfFile      生成文件路径
     */
    public  void template(String templateFile, Map<String, String> args, String pdfFile) {
        FileOutputStream output = null;
        try {
            // 读取模板文件,填充模板参数
            Configuration freemarkerCfg = new Configuration(Configuration.VERSION_2_3_30);
            freemarkerCfg.setTemplateLoader(new ClassTemplateLoader(PDFUtil.class, "/templates/"));
            Template template = freemarkerCfg.getTemplate(templateFile);
            StringWriter out = new StringWriter();
            if (args != null && args.size() > 0) {
                template.process(args, out);
            }
            String html = out.toString();

            // 设置字体以及字符编码
            ConverterProperties props = new ConverterProperties();
            FontProvider fontProvider = new FontProvider();
            PdfFont sysFont = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H", false);
            fontProvider.addFont(sysFont.getFontProgram(), "UniGB-UCS2-H");
            fontProvider.addStandardPdfFonts();
            fontProvider.addFont("templates/simsun.ttc");
            fontProvider.addFont("templates/STHeitibd.ttf");
            props.setFontProvider(fontProvider);
            props.setCharset("utf-8");

            // 转换为PDF文档
            if (pdfFile.indexOf("/") > 0) {
                File path = new File(pdfFile.substring(0, pdfFile.indexOf("/")));
                if (!path.exists()) {
                    path.mkdirs();
                }
            }
            output = new FileOutputStream(new File(pdfFile));
            PdfDocument pdf = new PdfDocument(new PdfWriter(output));
            pdf.setDefaultPageSize(PageSize.A4);
            Document document = HtmlConverter.convertToDocument(html, pdf, props);
            document.getRenderer().close();
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    public static void main(String[] args) {
//        Map<String, Object> para = new HashMap<String, Object>();
//        para.put("txid", "12344444dsdsdwssdf");
//        para.put("username", "123321");
//        para.put("name","钱贤松");
//        para.put("hash","sdfghjfghj");
//        para.put("title","weqrqwer");
//        para.put("timestamp","2022-9-22 20:00");
//        template("certi.html", para, "tmp/" + System.currentTimeMillis() + ".pdf");
//    }

}
