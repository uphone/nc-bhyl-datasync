/*jadclipse*/// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.

package nc.impl.iufo.freereport;

import com.ufida.dataset.Context;
import com.ufida.dataset.IContext;
import com.ufida.iufo.pub.tools.AppDebug;
import com.ufida.iufo.table.model.*;
import com.ufida.report.anareport.exec.*;
import com.ufida.report.anareport.model.FrExcContextUtil;
import com.ufida.report.anareport.model.SmartFilterDefs;
import com.ufida.report.anareport.util.FreeFieldConverter;
import com.ufida.report.anareport.util.FreeReportModuleTypeUtil;
import com.ufida.zior.perfwatch.PerfWatch;
import com.ufsoft.report.SeriImageIcon;
import com.ufsoft.report.SeriImageIconDataBox;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.ImageIcon;
import nc.bs.framework.common.NCLocator;
import nc.itf.iufo.freereport.IFreeSmartQryService;
import nc.itf.smart.ISmartFilterService;
import nc.pub.smart.cache.SmartModelCache;
import nc.pub.smart.data.DataSet;
import nc.pub.smart.data.SmartModelData;
import nc.pub.smart.exception.SmartException;
import nc.pub.smart.filter.SmartFilterModel;
import nc.pub.smart.metadata.MetaData;
import nc.pub.smart.model.SmartModel;
import nc.pub.smart.model.descriptor.*;
import nc.vo.ml.AbstractNCLangRes;
import nc.vo.ml.NCLangRes4VoTransl;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import uap.impl.bq.report.complex.calculate.FrCpxCalculateServiceImpl;
import uap.itf.bq.pub.IBQSmartQueryService;
import uap.pub.bq.report.complex.calculate.FrCpxResultDataSet;
import uap.pub.bq.swchart.context.SWChartContext;
import uap.pub.bq.swchart.exception.SWChartException;
import uap.pub.bq.swchart.exception.SWChartRuntimeException;
import uap.pub.bq.swchart.result.BaseSWChartResult;
import uap.pub.bq.swchart.result.ISWChartResult;
import uap.pub.bq.swchart.support.exe.SWChartExeSupport;
import uap.pub.bq.swchart.support.request.SWChartQueryReqSupport;
import uap.pub.bq.swchart.util.SWChartRequestUtils;
import uap.vo.bq.swchart.model.ISwChartModel;
import uap.vo.bq.swchart.model.runtime.ISWChartRuntimeModel;

public class FreeSmartQueryImpl
    implements IFreeSmartQryService
{

    public FreeSmartQueryImpl()
    {
    }

    public FreeRemoteResult provideDataSet(FreeRemoteParam param)
        throws SmartException
    {
        PerfWatch pw;
        pw = new PerfWatch(NCLangRes4VoTransl.getNCLangRes().getStrByID("1413006_0", "01413006-0798"));
        pw.appendMessage(param);
        FreeRemoteResult res;
        FreeRemoteResult freeremoteresult1;
        List tableQueryParams = new ArrayList();
        List chartQueryParams = new ArrayList();
        List cpxAreaQueryParams = new ArrayList();
        doSeparateparams(param, tableQueryParams, chartQueryParams, cpxAreaQueryParams);
        ISWChartResult swChartResults[] = excuteunifyChart(chartQueryParams);
        FrCpxResultDataSet cpxAreaResults[] = excuteCpxAreaQuery(cpxAreaQueryParams);
        FreeQueryOneParam queryDatas[] = param.createSmartModelDatas();
        DataSet results[] = null;
        if(queryDatas != null)
        {
            int count = queryDatas.length;
            SmartModelData smartDatas[] = new SmartModelData[count];
            for(int i = 0; i < count; i++)
                smartDatas[i] = queryDatas[i].getSmartModelData();

            IBQSmartQueryService srv = getBQSmartQueryService();
            results = srv.provideData(smartDatas);
            for(int i = 0; i < count; i++)
            {
                results[i] = queryDatas[i].getDataSet(results[i], param.getCache());
                if(param.getFreeParam().length > i && param.getFreeParam()[i] != null && param.getFreeParam()[i].getContext() != null)
                {
                    Object pictureAnaRepFields = param.getFreeParam()[i].getContext().getAttribute("key_field_is_picture");
                    if(pictureAnaRepFields instanceof List)
                        execFieldIsPicture((List)pictureAnaRepFields, results[i], param.getFreeParam()[i].getFieldConverter());
                }
                if(results[i] == null || results[i].isEmpty())
                {
                    DataSet emptyData = new DataSet();
                    int dataLenth = results[i].getMetaData() == null ? getDataLenth(queryDatas[i].getFreeQueryParam()) : results[i].getMetaData().getFieldNum();
                    Object datas[][] = new Object[1][dataLenth];
                    emptyData.setDatas(datas);
                    emptyData.setMetaData(results[i] == null || results[i].getMetaData() == null ? queryDatas[i].getFreeQueryParam().getSmartModel().getMetaData() : results[i].getMetaData());
                    results[i] = emptyData;
                }
            }

        }
        res = new FreeRemoteResult(param.getCache(), results);
        res.setChartResult(swChartResults);
        res.setCpxResult(cpxAreaResults);
        boolean return2Client = true;
        if(results != null)
        {
            for(int i = 0; i < results.length; i++)
            {
                DataSet ds = results[i];
                if(ds != null && ds.getPaginal() != null)
                {
                    if(FreePolicyUtil.isQuerySubData(queryDatas[i].getContext()) || !FreePolicyUtil.isShowException(queryDatas[i].getContext()))
                        continue;
                    FreePolicy qPolicy = queryDatas[i].getFreeQueryParam().getPolicy();
                    if(qPolicy != null && !qPolicy.isDataLimit())
                        return2Client = false;
                    else
                        res.setIsException(i);
                    break;
                }
                if(!FreePolicyUtil.isQueryFirtSubData(queryDatas[i].getContext()))
                    continue;
                Integer dsCount = (Integer)queryDatas[i].getContext().getAttribute("FreeReport_subDataSet_allcount");
                FreePolicy policy = FreePolicyUtil.getPolicy(queryDatas[i].getContext());
                if(policy.isDataLimit())
                    dsCount = Integer.valueOf(queryDatas[i].getPolicyDataRow());
                if(dsCount != null)
                {
                    FreeDataPaginal p = new FreeDataPaginal(dsCount.intValue());
                    p.setPageSize(queryDatas[i].getPolicyDataRow());
                    ds.setPaginal(p);
                }
            }

        }
        if(return2Client) return null;
        FreeQueryParam params[] = param.getFreeParam();
        FreeQueryParam arr$[] = params;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            FreeQueryParam pp = arr$[i$];
            if(pp.getPolicy() == null)
                pp.setQueryPolicy(FreePolicyFactory.getDefaultPolicy());
            pp.getPolicy().setDataType(1);
            pp.getPolicy().setclearTempType(2);
        }

        try{
        	freeremoteresult1 = provideDataSet(param);
        return freeremoteresult1;
        }catch(Exception ex){
        	throw new RuntimeException(ex);
        }finally {
        	pw.stop();
        }
//        pw.stop();
//        FreeRemoteResult freeremoteresult = res;
//        pw.stop();
//        return freeremoteresult;
//        Exception exception;
//        exception;
//        pw.stop();
//        throw exception;
    }

    private FrCpxResultDataSet[] excuteCpxAreaQuery(List cpxAreaQueryParams)
    {
        FrCpxResultDataSet cpxResults[] = new FrCpxResultDataSet[cpxAreaQueryParams.size()];
        FrCpxCalculateServiceImpl cpxService = new FrCpxCalculateServiceImpl();
        for(int i = 0; i < cpxAreaQueryParams.size(); i++)
        {
            FreeCpxQueryParam freeCpxQueryParam = (FreeCpxQueryParam)cpxAreaQueryParams.get(i);
            Descriptor descArray[] = freeCpxQueryParam.getDescArray();
            IContext context = freeCpxQueryParam.getContext();
            Descriptor newDescArray[] = descArray;
            Object smartFilterDefs = context.getAttribute("key_smart_filter_defs");
            if(smartFilterDefs instanceof SmartFilterDefs)
            {
                SmartFilterDefs filterDefs = (SmartFilterDefs)smartFilterDefs;
                if(!ArrayUtils.isEmpty(filterDefs.getFiltersPk()))
                {
                    ISmartFilterService smartFilterService = (ISmartFilterService)NCLocator.getInstance().lookup(nc.itf.smart.ISmartFilterService.class);
                    try
                    {
                        String arr$[] = filterDefs.getFiltersPk();
                        int len$ = arr$.length;
                        for(int i$ = 0; i$ < len$; i$++)
                        {
                            String filterPk = arr$[i$];
                            if(!StringUtils.isEmpty(filterPk))
                            {
                                SmartFilterModel smartFilterModel = smartFilterService.loadFilterModel(filterPk);
                                newDescArray = (Descriptor[])(Descriptor[])ArrayUtils.addAll(newDescArray, smartFilterModel.getDescriptors());
                            }
                        }

                    }
                    catch(SmartException e)
                    {
                        AppDebug.error(e);
                    }
                }
            }
            context.setAttribute("key_frcpx_filterdesc", newDescArray);
            cpxResults[i] = cpxService.calculate(freeCpxQueryParam.getFrCpxModel(), context);
        }

        return cpxResults;
    }

    private void doSeparateparams(FreeRemoteParam param, List tableQueryParams, List chartQueryParams, List cpxAreaQueryParams)
    {
        FreeQueryParam freeParam[] = param.getFreeParam();
        if(freeParam == null)
            return;
        FreeQueryParam arr$[] = freeParam;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            FreeQueryParam freeQueryParam = arr$[i$];
            if(freeQueryParam instanceof FreeChartQueryParam)
            {
                chartQueryParams.add((FreeChartQueryParam)freeQueryParam);
                continue;
            }
            if(freeQueryParam instanceof FreeCpxQueryParam)
                cpxAreaQueryParams.add((FreeCpxQueryParam)freeQueryParam);
            else
                tableQueryParams.add(freeQueryParam);
        }

        param.setFreeParam((FreeQueryParam[])tableQueryParams.toArray(new FreeQueryParam[tableQueryParams.size()]));
    }

    private void execFieldIsPicture(List pictureAnaRepFields, DataSet dataSet, FreeFieldConverter freeFieldConverter)
    {
        if(dataSet == null || pictureAnaRepFields == null || pictureAnaRepFields.isEmpty())
            return;
        List pictureFieldIndexes = new ArrayList();
        Iterator i$ = pictureAnaRepFields.iterator();
        do
        {
            if(!i$.hasNext())
                break;
            String pictureAnaRepField = (String)i$.next();
            if(freeFieldConverter != null)
                pictureAnaRepField = freeFieldConverter.getConvertName(pictureAnaRepField);
            int pictureFieldIndex = dataSet.getMetaData().getIndex(pictureAnaRepField);
            if(pictureFieldIndex > -1)
                pictureFieldIndexes.add(Integer.valueOf(pictureFieldIndex));
        } while(true);
        if(!pictureFieldIndexes.isEmpty())
        {
            Object newDatas[][] = new Object[dataSet.getCount()][];
            for(int i = 0; i < newDatas.length; i++)
            {
                newDatas[i] = new Object[dataSet.getRowData(i).length];
                for(int j = 0; j < newDatas[i].length; j++)
                    if(pictureFieldIndexes.contains(Integer.valueOf(j)))
                    {
                        Object data = dataSet.getRowData(i)[j];
                        if(data instanceof String)
                        {
                            URL url = null;
                            if(((String)data).indexOf("http") == 0)
                                try
                                {
                                    url = new URL((String)data);
                                }
                                catch(MalformedURLException e)
                                {
                                    AppDebug.error(e);
                                }
                            else
                            if(((String)data).indexOf("ref") == 0)
                                url = getClass().getClassLoader().getResource(((String)data).replace("ref://", ""));
                            if(url != null)
                            {
                                ImageIcon imageIcon = new ImageIcon(url);
                                if(8 == imageIcon.getImageLoadStatus())
                                    data = new SeriImageIconDataBox(data, new SeriImageIcon(imageIcon));
                            }
                        } else
                        if(data instanceof byte[])
                        {
                            ImageIcon imageIcon = new ImageIcon((byte[])(byte[])data);
                            if(8 == imageIcon.getImageLoadStatus())
                                data = new SeriImageIconDataBox(data, new SeriImageIcon(imageIcon));
                        }
                        newDatas[i][j] = data;
                    } else
                    {
                        newDatas[i][j] = dataSet.getRowData(i)[j];
                    }

            }

            dataSet.setDatas(newDatas);
        }
    }

    private ISWChartResult[] excuteunifyChart(List chartQueryParams)
    {
        ISWChartResult swChartResults[] = new BaseSWChartResult[chartQueryParams.size()];
        for(int i = 0; i < swChartResults.length; i++)
        {
            ISWChartResult result = getOneChartResult((FreeChartQueryParam)chartQueryParams.get(i));
            swChartResults[i] = result;
        }

        return swChartResults;
    }

    private ISWChartResult getOneChartResult(FreeChartQueryParam chartQueryParam)
    {
        ISwChartModel swChartModel = chartQueryParam.getSwChartModel();
        if(swChartModel == null)
            return null;
        SWChartContext chartContext = getChartContext(chartQueryParam);
        if(swChartModel != null)
        {
            ISWChartRuntimeModel swChartRtModel = SWChartRequestUtils.getSWChartRtModel(swChartModel.getChartId(), chartContext);
            if(swChartRtModel != null)
                swChartModel.setSWChartRuntimeModel(swChartRtModel);
            SWChartRequestUtils.removeSWChartRtModel(swChartModel.getChartId(), chartContext);
        }
        SWChartQueryReqSupport updateReqSupport = new SWChartQueryReqSupport(swChartModel, chartContext);
        SWChartExeSupport exeSupport = new SWChartExeSupport();
        ISWChartResult chartResult = null;
        try
        {
            chartResult = exeSupport.exeChartRequest(updateReqSupport.genChartRequest());
            return chartResult;
        }
        catch(SWChartException e1)
        {
            AppDebug.error(e1);
            throw new SWChartRuntimeException(NCLangRes4VoTransl.getNCLangRes().getStrByID("0502001_0", "00502001-0008"), e1);
        }
    }

    private SWChartContext getChartContext(FreeChartQueryParam chartQueryParam)
    {
        IContext remoteContext = chartQueryParam.getContext();
        SWChartContext chartContext = new SWChartContext((Context)remoteContext);
        if(FreeReportModuleTypeUtil.isModuleOfBq(remoteContext))
            chartContext.setSWChartLocation(1);
        else
            chartContext.setSWChartLocation(0);
        Descriptor descArray[] = chartQueryParam.getDescArray();
        String areaPk = chartQueryParam.getAreaPK();
        SmartModel smartModel = chartQueryParam.getSmartModel() != null ? chartQueryParam.getSmartModel() : getSmartModel(chartQueryParam.getSmartID());
        FrExcContextUtil.delFrExcChartContext(chartContext, remoteContext, descArray, areaPk, smartModel);
        return chartContext;
    }

    private SmartModel getSmartModel(String smartID)
    {
        return SmartModelCache.getInstance().getModel(smartID);
    }

    private int getDataLenth(FreeQueryParam freeQueryParam)
    {
        int lenth = 0;
        Descriptor descriptors[] = freeQueryParam.getDescArray();
        Descriptor arr$[] = descriptors;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            Descriptor descriptor = arr$[i$];
            if(descriptor instanceof SelectColumnDescriptor)
            {
                nc.pub.smart.metadata.Field fields[] = ((SelectColumnDescriptor)descriptor).getFields();
                if(fields != null)
                    lenth += fields.length;
                continue;
            }
            if(!(descriptor instanceof AggrDescriptor))
                continue;
            nc.pub.smart.model.descriptor.AggrItem aggItems[] = ((AggrDescriptor)descriptor).getAggrFields();
            if(aggItems != null)
                lenth += aggItems.length;
            nc.pub.smart.model.descriptor.GroupItem groupItems[] = ((AggrDescriptor)descriptor).getGroupFields();
            if(groupItems != null)
                lenth += groupItems.length;
        }

        return lenth;
    }

    public FreeRemoteResult provideCount(FreeRemoteParam param)
        throws SmartException
    {
        FreeQueryOneParam queryDatas[] = param.createSmartModelDatas();
        if(queryDatas == null || queryDatas.length == 0)
            return null;
        int count = queryDatas.length;
        SmartModelData dsExec[] = new SmartModelData[count];
        for(int i = 0; i < count; i++)
            dsExec[i] = queryDatas[i].getSmartModelData();

        IBQSmartQueryService srv = getBQSmartQueryService();
        try
        {
            Integer results[] = srv.provideCount(dsExec);
            FreeRemoteResult result = new FreeRemoteResult(param.getCache(), results);
            return result;
        }
        catch(SmartException ex)
        {
            AppDebug.debug(ex);
        }
        return null;
    }

    private IBQSmartQueryService getBQSmartQueryService()
    {
        return (IBQSmartQueryService)NCLocator.getInstance().lookup(uap.itf.bq.pub.IBQSmartQueryService.class);
    }
}


/*
	DECOMPILATION REPORT

	Decompiled from: C:\ab\NC\NC65\bhyl\nc65home\modules\bqrtufr\META-INF\lib\bqrtufr_ufofrLevel-1.jar
	Total time: 63 ms
	Jad reported messages/errors:
Overlapped try statements detected. Not all exception handlers will be resolved in the method provideDataSet
Couldn't fully decompile method provideDataSet
Couldn't resolve all exception handlers in method provideDataSet
	Exit status: 0
	Caught exceptions:
*/