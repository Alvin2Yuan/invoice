package org.slong.tools;

import cn.hutool.core.util.XmlUtil;
import cn.hutool.json.JSONObject;
import com.itextpdf.kernel.pdf.PdfXrefTable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dom4j.Node;
import org.jetbrains.annotations.NotNull;
import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.reader.ContentExtractor;
import org.ofdrw.reader.OFDReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {

        String folderPath="D:\\dzfp";
        if(args.length>0){
            folderPath=args[0];
        }
        File folder=new File(folderPath);
        if(!folder.exists()){
            System.out.println("没有找到需要处理的文件夹");
        }
        for(File subFile: folder.listFiles()){
            String name= subFile.getName();
            if(name.endsWith(".ofd")){
              // JSONObject ofdData= getOfdFileData(subFile);
            }else if(name.endsWith(".pdf")){
               JSONObject pdfData=  getPdfFileData(subFile);
            }else if(name.endsWith(".xml")){
                JSONObject xmlData=  getXmlFileData(subFile);
            }
        }
    }


    private static JSONObject getOfdFileData(File file) throws IOException {
        // 使用OfdReader读取OFD文件
        OFDReader reader = new OFDReader(new FileInputStream(file));

        // 默认获取第一页
        ContentExtractor extractor = new ContentExtractor(reader);

        List<String> pageContent = extractor.getPageContent(1);
        JSONObject data=new JSONObject();
        data.set("发票号码",pageContent.get(0));
        data.set("开票日期",pageContent.get(1));
        data.set("购方名称",pageContent.get(2));
        data.set("销方名称",pageContent.get(4));
        data.set("金额信息",pageContent.get(12));
        data.set("备注",pageContent.get(13));
//        for(String row:pageContent){
//            System.out.println(row);
//
//        }
        System.out.println(data);
        return data;
    }

    private static JSONObject getPdfFileData(File file) throws IOException {
        // 使用PDDocument读取PDF文件
        PDDocument document = PDDocument.load(file);

        // 使用PDFTextStripper提取文本内容
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String content= stripper.getText(document);
        String[] rows=content.split("\\r\\n");
        JSONObject data=new JSONObject();
        int rowNum=0;
        boolean flag=false;
        boolean remarkFlag=false;
        List<String> shopNameList=new ArrayList<String>();
        for(String row :rows){
            rowNum++;
            if(row.length()>4&&!row.contains("项目名称")){
               // System.out.println(row);
                if(row.contains("发票号码：")){
                    String invoiceNumber=row.substring(row.indexOf("发票号码：")+5);

                    data.set("发票号码",invoiceNumber);
                }
                if(row.contains("开票日期：")){
                    String invoiceDate=row.substring(row.indexOf("开票日期：")+5);
                    data.set("开票日期",invoiceDate);
                }
                if(row.contains("购 名称：")){
                    String buyerInfo=row.substring(row.indexOf("购 名称：")+5,row.indexOf("销 名称："));
                    System.out.println("购方名称：" + buyerInfo);
                    data.set("购方名称",buyerInfo);
                }
                if(row.contains("销 名称：")){
                    String sellerInfo=row.substring(row.indexOf("销 名称：")+5);
                    System.out.println("销 名称：" + sellerInfo);
                    data.set("销方名称",sellerInfo);
                }

                if(row.contains("合 计")){
                    flag=false;
                    continue;
                }
                if(rowNum==9||flag){
                    flag=true;
                    String itemInfo=row.substring(0,row.indexOf(" "));
                    System.out.println("商品信息：" + itemInfo);
                    shopNameList.add(itemInfo);
                    System.out.println("商品信息集合：" + shopNameList);
                }
                if(row.contains("（小写）")){
                    remarkFlag=true;
                    String amountInfo=row.substring(row.indexOf("（小写）")+5);
                    System.out.println("金额信息：" + amountInfo);
                    data.set("金额信息",amountInfo);

                }
                if(row.contains("开票人")){
                    remarkFlag=false;
                }
                if(remarkFlag){
                    data.set("备注",row);
                }

            }
        }


       // System.out.println(content);

        // 关闭PDDocument
        document.close();

        return data;
    }

    private static JSONObject getXmlFileData(File file){
        JSONObject data=new JSONObject();
        Document document=XmlUtil.readXML(file);
        Element element= document.getDocumentElement();
      Element rootElement= XmlUtil.getRootElement(document);


       Element invoiceDataElement= XmlUtil.getElement(rootElement,"EInvoiceData");
        Element sellerInformation= XmlUtil.getElement(invoiceDataElement,"SellerInformation");
        String sellerName= XmlUtil.getElement(sellerInformation,"SellerName").getTextContent();
        Element taxSupervisionInfo= XmlUtil.getElement(rootElement,"TaxSupervisionInfo");
        Element element1= XmlUtil.getElement(taxSupervisionInfo,"InvoiceNumber");
        System.out.println(element1.getTextContent());

        String invoiceNumber= XmlUtil.getElement(taxSupervisionInfo,"InvoiceNumber").getTextContent();
        String invoiceDate= XmlUtil.getElement(taxSupervisionInfo,"IssueTime").getTextContent();

        Element buyerInformation= XmlUtil.getElement(invoiceDataElement,"BuyerInformation");
        String buyerName= XmlUtil.getElement(buyerInformation,"BuyerName").getTextContent();

        Element issuItemInformation= XmlUtil.getElement(invoiceDataElement,"IssuItemInformation");
        String totaltaxIncludedAmount= XmlUtil.getElement(issuItemInformation,"TotaltaxIncludedAmount").getTextContent();

        Element additionalInformation= XmlUtil.getElement(invoiceDataElement,"AdditionalInformation");
        String remark= XmlUtil.getElement(additionalInformation,"Remark").getTextContent();
        data.set("发票号码",invoiceNumber);
        data.set("开票日期",invoiceDate);
        data.set("购方名称",sellerName);
        data.set("销方名称",buyerName);
        data.set("金额信息",totaltaxIncludedAmount);
        data.set("备注",remark);
       return data;
    }
}
