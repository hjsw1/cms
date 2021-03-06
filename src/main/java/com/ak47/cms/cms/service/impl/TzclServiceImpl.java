package com.ak47.cms.cms.service.impl;

import com.ak47.cms.cms.dao.TzclJpaRepository;
import com.ak47.cms.cms.dto.TzclDto;
import com.ak47.cms.cms.entity.Tzcl;
import com.ak47.cms.cms.enums.ManageBankEnum;
import com.ak47.cms.cms.enums.ManageCountryEnum;
import com.ak47.cms.cms.result.PageResult;
import com.ak47.cms.cms.result.Result;
import com.ak47.cms.cms.result.ResultUtils;
import com.ak47.cms.cms.service.TzclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wb-cmx239369 on 2017/11/6.
 */
@Service
public class TzclServiceImpl implements TzclService{
    @Autowired
    private TzclJpaRepository tzclJpaRepository;
    @Autowired
    private EntityManager entityManager;
    private final String PRE_TZCL_SQL = "SELECT t.* FROM Tzcl t WHERE t.country = {} AND t.bank = {} and t.{} <> 0 and t.is_deleted = 'n' order by t.open_date desc limit 1,1";
    private final String TZCL_SQL = "SELECT t.* FROM Tzcl t WHERE t.country = {} AND t.bank = {} and t.{} <> 0 and t.is_deleted = 'n' order by t.open_date desc limit 1";

    private final BigDecimal HUNDRED = new BigDecimal(100);
    @Override
    public Tzcl save(Tzcl tzcl) {
        return tzclJpaRepository.save(tzcl);
    }

    @Override
    public List<Tzcl> findAll() {
        return tzclJpaRepository.findAll();
    }

    @Override
    public void delete(Long id) {
        tzclJpaRepository.deleteById(id);
    }

    @Override
    public Tzcl findOne(Long id) {
        return tzclJpaRepository.getOne(id);
    }

    @Override
    public Result<PageResult<TzclDto>> findPage(PageResult<Tzcl> pageResult) {
        if(pageResult == null) {
            ResultUtils.instancePageResult(0,0,0,null,"页码获取失败",false);
        }
//        List<TzclDto> tzclDtos = tzclJpaRepository.findTable((pageResult.getPageNumber() - 1)*pageResult.getPageSize(), pageResult.getPageSize());
        List<ManageCountryEnum> countryEnums = Arrays.asList(ManageCountryEnum.values());
        List<ManageBankEnum> bankEnums = Arrays.asList(ManageBankEnum.values());
        PageRequest pageRequest = new PageRequest(pageResult.getPageNumber()-1, pageResult.getPageSize());
        Page<Tzcl> tzcls =  tzclJpaRepository.findTzclTable(pageRequest);
        List<TzclDto> tzclDtos = toDto(tzcls.getContent());
        return ResultUtils.instancePageResult(tzcls.getNumber()+1,tzcls.getSize(),tzcls.getTotalElements(),tzclDtos,"获取成功",true);

//        tzclDtos.forEach(t->{
//        });
//        return ResultUtils.instancePageResult(pageResult.getPageNumber(),pageResult.getPageSize(),tzclJpaRepository.findTableCount(),tzclDtos,"获取成功",true);
    }

    private String getPreTzclSql(String sqlStr,String...wheres){
        StringBuilder sql = new StringBuilder(sqlStr);
        for(String where:wheres){
            sql = sql.replace(sql.indexOf("{"),sql.indexOf("}")+1,where);
        }
        return sql.toString();
    }

    private List<TzclDto> toDto(List<Tzcl> tzcls){
        List<ManageCountryEnum> countryEnums = Arrays.asList(ManageCountryEnum.values());
        List<ManageBankEnum> bankEnums = Arrays.asList(ManageBankEnum.values());
        List<TzclDto> tzclDtos = new ArrayList<>();
        tzcls.forEach(tt->{
            TzclDto t = new TzclDto(tt);
            List list = entityManager.createNativeQuery(getPreTzclSql(PRE_TZCL_SQL,t.getCountry()+"",t.getBank()+"","r"),Tzcl.class).getResultList();
            Tzcl preR = list!=null&&list.size()>0?(Tzcl)list.get(0):null;
            t.setPerR(preR==null?"0":getNum(preR.getR()).toString());
            list = entityManager.createNativeQuery(getPreTzclSql(PRE_TZCL_SQL,t.getCountry()+"",t.getBank()+"","cpi"),Tzcl.class).getResultList();
            Tzcl preCpi = list!=null&&list.size()>0?(Tzcl)list.get(0):null;
            t.setPerCpi(preCpi==null?new BigDecimal(0):preCpi.getCpi().multiply(HUNDRED));
            list = entityManager.createNativeQuery(getPreTzclSql(PRE_TZCL_SQL,t.getCountry()+"",t.getBank()+"","gdp"),Tzcl.class).getResultList();
            Tzcl preGdp = list!=null&&list.size()>0?(Tzcl)list.get(0):null;
            t.setPerGdp(preGdp==null?new BigDecimal(0):preGdp.getGdp().multiply(HUNDRED));
            list = entityManager.createNativeQuery(getPreTzclSql(PRE_TZCL_SQL,t.getCountry()+"",t.getBank()+"","un"),Tzcl.class).getResultList();
            Tzcl preUn = list!=null&&list.size()>0?(Tzcl)list.get(0):null;
            t.setPerUn(preUn==null?new BigDecimal(0):preUn.getUn().multiply(HUNDRED));
            if(t.getCpi() == null){
                list = entityManager.createNativeQuery(getPreTzclSql(TZCL_SQL,t.getCountry()+"",t.getBank()+"","cpi"),Tzcl.class).getResultList();
                Tzcl cpi = list!=null&&list.size()>0?(Tzcl)list.get(0):null;
                t.setCpi(cpi==null?new BigDecimal(0):cpi.getCpi().multiply(HUNDRED));
            }else{
                t.setCpi(t.getCpi().multiply(HUNDRED));
            }
            t.setCpiDiff(t.getCpi().subtract(t.getPerCpi()));
            if(t.getGdp() == null){
                list = entityManager.createNativeQuery(getPreTzclSql(TZCL_SQL,t.getCountry()+"",t.getBank()+"","gdp"),Tzcl.class).getResultList();
                Tzcl gdp = list!=null&&list.size()>0?(Tzcl)list.get(0):null;
                t.setGdp(gdp==null?new BigDecimal(0):gdp.getGdp().multiply(HUNDRED));
            }else{
                t.setGdp(t.getGdp().multiply(HUNDRED));
            }
            t.setGdpDiff(t.getGdp().subtract(t.getPerGdp()));
            if(t.getUn() == null){
                list = entityManager.createNativeQuery(getPreTzclSql(TZCL_SQL,t.getCountry()+"",t.getBank()+"","un"),Tzcl.class).getResultList();
                Tzcl un = list!=null&&list.size()>0?(Tzcl)list.get(0):null;
                t.setUn(un==null?new BigDecimal(0):un.getUn().multiply(HUNDRED));
            }else{
                t.setUn(t.getUn().multiply(HUNDRED));
            }
            if(t.getR() != null){
                t.setR(getNum(t.getR()).toString());
            }
            t.setUnDiff(t.getUn().subtract(t.getPerUn()));
            String[] rs = t.getR().split(",");
            String[] perRs = t.getPerR().split(",");
            BigDecimal r = getNum(rs[0],false);
            for(String str : rs){
                BigDecimal r1 = getNum(str,false);
                r = r.compareTo(r1) >0?r:r1;
            }
            BigDecimal perR = getNum(perRs[0],false);
            for(String str : perRs){
                BigDecimal perR1 = getNum(str,false);
                perR = perR.compareTo(perR1) >0?perR:perR1;
            }
            t.setrDiff(r.subtract(perR));
            t.setGdpStatus(t.getGdpDiff().compareTo(new BigDecimal(0))>0?"Positive":t.getGdpDiff().compareTo(new BigDecimal(0))==0?"Neutral":"Negative");
            t.setCpiStatus(t.getCpiDiff().compareTo(new BigDecimal(0))>0?"Positive":t.getCpiDiff().compareTo(new BigDecimal(0))==0?"Neutral":"Negative");
            t.setUnStatus(t.getUnDiff().compareTo(new BigDecimal(0))>0?"Positive":t.getUnDiff().compareTo(new BigDecimal(0))==0?"Neutral":"Negative");
            t.setrStatus(t.getrDiff().compareTo(new BigDecimal(0))>0?"Positive":t.getrDiff().compareTo(new BigDecimal(0))==0?"Neutral":"Negative");
            int status = t.getGdpDiff().compareTo(new BigDecimal(0))+t.getCpiDiff().compareTo(new BigDecimal(0))+t.getUnDiff().compareTo(new BigDecimal(0));
            t.setScenario(status==3?"++":status>0?"+":status == 0?"0":status>-3?"-":"--");
            tzclDtos.add(t);
        });
        return tzclDtos;
    }

    private BigDecimal getNum(String str){
        return getNum(str,true);
    }
    private BigDecimal getNum(String str,boolean isMultiply){
        try {
            if(isMultiply)
                return new BigDecimal(str).multiply(HUNDRED);
            return new BigDecimal(str);
        } catch (Exception e) {
            Pattern p = Pattern.compile("(-)*(\\d+\\.\\d+)");
            Matcher m = p.matcher(str);
            if (m.find()) {
                //如果有相匹配的,则判断是否为null操作
                //group()中的参数：0表示匹配整个正则，1表示匹配第一个括号的正则,2表示匹配第二个正则,在这只有一个括号,即1和0是一样的
                str = m.group(0) == null ? "0" : m.group(0);
            } else {
                //如果匹配不到小数，就进行整数匹配
                p = Pattern.compile("(-)*(\\d+)");
                m = p.matcher(str);
                if (m.find()) {
                    //如果有整数相匹配
                    str = m.group(0) == null ? "" : m.group(0);
                } else {
                    //如果没有小数和整数相匹配,即字符串中没有整数和小数，就设为空
                    str = "0";
                }
            }
        }

        return new BigDecimal(str);
    }
}
