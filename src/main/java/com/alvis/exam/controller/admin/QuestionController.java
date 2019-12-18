package com.alvis.exam.controller.admin;

import com.alibaba.fastjson.JSON;
import com.alvis.exam.base.BaseApiController;
import com.alvis.exam.base.RestResponse;
import com.alvis.exam.base.SystemCode;
import com.alvis.exam.domain.Question;
import com.alvis.exam.domain.TextContent;
import com.alvis.exam.domain.enums.QuestionTypeEnum;
import com.alvis.exam.domain.question.QuestionObject;
import com.alvis.exam.service.QuestionService;
import com.alvis.exam.service.TextContentService;
import com.alvis.exam.utility.*;
import com.alvis.exam.viewmodel.admin.question.QuestionEditItemVM;
import com.alvis.exam.viewmodel.admin.question.QuestionEditRequestVM;
import com.alvis.exam.viewmodel.admin.question.QuestionPageRequestVM;
import com.alvis.exam.viewmodel.admin.question.QuestionResponseVM;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController("AdminQuestionController")
@RequestMapping(value = "/api/admin/question")
@AllArgsConstructor
public class QuestionController extends BaseApiController {

    private final QuestionService questionService;
    private final TextContentService textContentService;

    @RequestMapping(value = "/page", method = RequestMethod.POST)
    public RestResponse<PageInfo<QuestionResponseVM>> pageList(@RequestBody QuestionPageRequestVM model) {
        PageInfo<Question> pageInfo = questionService.page(model);
        PageInfo<QuestionResponseVM> page = PageInfoHelper.copyMap(pageInfo, q -> {
            QuestionResponseVM vm = modelMapper.map(q, QuestionResponseVM.class);
            vm.setCreateTime(DateTimeUtil.dateFormat(q.getCreateTime()));
            vm.setScore(ExamUtil.scoreToVM(q.getScore()));
            TextContent textContent = textContentService.selectById(q.getInfoTextContentId());
            QuestionObject questionObject = JsonUtil.toJsonObject(textContent.getContent(), QuestionObject.class);
            String clearHtml = HtmlUtil.clear(questionObject.getTitleContent());
            vm.setShortTitle(clearHtml);
            return vm;
        });
        return RestResponse.ok(page);
    }

    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public RestResponse edit(@RequestBody @Valid QuestionEditRequestVM model) {
        RestResponse validQuestionEditRequestResult = validQuestionEditRequestVM(model);
        if (validQuestionEditRequestResult.getCode() != SystemCode.OK.getCode()) {
            return validQuestionEditRequestResult;
        }

        if (null == model.getId()) {
            questionService.insertFullQuestion(model, getCurrentUser().getId());
        } else {
            questionService.updateFullQuestion(model);
        }

        return RestResponse.ok();
    }
    @RequestMapping(value = "/uploadQue", method = RequestMethod.POST)
    public RestResponse uploadQue(HttpServletRequest request) {
        MultipartHttpServletRequest multipartHttpServletRequest = (MultipartHttpServletRequest) request;
        MultipartFile multipartFile = multipartHttpServletRequest.getFile("question");
        try (InputStream inputStream = multipartFile.getInputStream()) {
            InputStreamReader isr = new InputStreamReader(inputStream, "GBK");
            BufferedReader br = new BufferedReader(isr);
            StringBuilder result = new StringBuilder();
            String s = null;
            while((s = br.readLine())!=null){//使用readLine方法，一次读一行
                result.append(System.lineSeparator()+s);
            }
            br.close();
            String s1 = result.toString();
            String[] split = s1.split("\n");
            for (String s2 : split) {
                String pattern = "[1-9][0-9]?、.*";
                boolean matches = s2.matches(pattern);
                if(matches){
                    System.out.println(s2);
                }
            }
            return RestResponse.ok(null);
        } catch (IOException e) {
            return RestResponse.fail(2, e.getMessage());
        }
    }

    @RequestMapping(value = "/select/{id}", method = RequestMethod.POST)
    public RestResponse<QuestionEditRequestVM> select(@PathVariable Integer id) {
        QuestionEditRequestVM newVM = questionService.getQuestionEditRequestVM(id);
        return RestResponse.ok(newVM);
    }


    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    public RestResponse delete(@PathVariable Integer id) {
        Question question = questionService.selectById(id);
        question.setDeleted(true);
        questionService.updateByIdFilter(question);
        return RestResponse.ok();
    }

    private RestResponse validQuestionEditRequestVM(QuestionEditRequestVM model) {
        int qType = model.getQuestionType().intValue();
        boolean requireCorrect = qType == QuestionTypeEnum.SingleChoice.getCode() || qType == QuestionTypeEnum.TrueFalse.getCode();
        if (requireCorrect) {
            if (StringUtils.isBlank(model.getCorrect())) {
                String errorMsg = ErrorUtil.parameterErrorFormat("correct", "不能为空");
                return new RestResponse<>(SystemCode.ParameterValidError.getCode(), errorMsg);
            }
        }

        if (qType == QuestionTypeEnum.GapFilling.getCode()) {
            Integer fillSumScore = model.getItems().stream().mapToInt(d -> ExamUtil.scoreFromVM(d.getScore())).sum();
            Integer questionScore = ExamUtil.scoreFromVM(model.getScore());
            if (!fillSumScore.equals(questionScore)) {
                String errorMsg = ErrorUtil.parameterErrorFormat("score", "空分数和与题目总分不相等");
                return new RestResponse<>(SystemCode.ParameterValidError.getCode(), errorMsg);
            }
        }
        return RestResponse.ok();
    }

    public static void main(String[] args) throws  Exception {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost("http://localhost:81/api/admin/question/edit");
        String path = "C:\\Users\\Administrator\\Desktop\\question.txt";
        FileInputStream fis = new FileInputStream(new File(path));
        InputStreamReader isr = new InputStreamReader(fis, "GBK");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder result = new StringBuilder();
        String s = null;
        while((s = br.readLine())!=null){//使用readLine方法，一次读一行
            result.append(System.lineSeparator()+s);
        }
        br.close();
        String s1 = result.toString();
        String[] split = s1.split("\r\n");
        List<String> collect = Arrays.asList(split);
        List<String> questions = collect.stream().filter(x -> !x.equals("")).collect(Collectors.toList());
        List<HashMap> lists = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            String s2 = questions.get(i);
            String pattern = "[1-9][0-9]?、.*";
            boolean matches = s2.matches(pattern);
            if(matches){
                String a = questions.get(i + 1).substring(2);
                String b = questions.get(i + 2).substring(2);
                String c = questions.get(i + 3).substring(2);
                String d = questions.get(i + 4).substring(2);
                String e = questions.get(i + 5).substring(2);
                String answer = questions.get(i + 6).substring(7);
                int next = 0;
                for (int j = i + 8; j < questions.size(); j++) {
                    if (questions.get(j).matches(pattern)) {
                        next = j;
                        break;
                    } else {
                        next = questions.size();
                    }
                }
                StringBuffer analysis = new StringBuffer();
                for (int n = i + 8; n < next; n++) {
                    analysis.append(questions.get(n));
                }
                HashMap<Object, Object> map = new HashMap<>();
                map.put("title", s2.substring(2));
                map.put("a", a);
                map.put("b", b);
                map.put("c", c);
                map.put("d", d);
                map.put("e", e);
                map.put("answer", answer);
                map.put("analysis", analysis);
                lists.add(map);
            }
        }
        for (HashMap list : lists) {
            Random random = new Random();
            int i = random.nextInt(4);
            QuestionEditRequestVM vm = new QuestionEditRequestVM();
            String title = list.get("title").toString();
            String analysis = list.get("analysis").toString();
            String answer = list.get("answer").toString();
            if (title.startsWith("、")) {
                title= title.substring(1);
            }
            vm.setTitle(title);
            vm.setAnalyze(analysis);
            vm.setCorrect(answer);
            vm.setScore("1");
            vm.setId(null);
            vm.setGradeLevel(1);
            vm.setDifficult(i+1);
            vm.setQuestionType(1);
            vm.setSubjectId(68);
            List<QuestionEditItemVM> selects = new ArrayList<>();
            for (int m = 0; m < 5; m++) {
                QuestionEditItemVM itemVM = new QuestionEditItemVM();
                if (m == 0) {
                    itemVM.setPrefix("A");
                    itemVM.setContent(list.get("a").toString());
                }
                if (m == 1) {
                    itemVM.setPrefix("B");
                    itemVM.setContent(list.get("b").toString());
                }
                if (m == 2) {
                    itemVM.setPrefix("C");
                    itemVM.setContent(list.get("c").toString());
                }
                if (m == 3) {
                    itemVM.setPrefix("D");
                    itemVM.setContent(list.get("d").toString());
                }
                if (m == 4) {
                    itemVM.setPrefix("E");
                    itemVM.setContent(list.get("e").toString());
                }
                selects.add(itemVM);
            }
            vm.setItems(selects);
            String jsonString = JSON.toJSONString(vm);
            StringEntity entity = new StringEntity(jsonString, "UTF-8");
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-Type", "application/json;charset=utf8");
            httpPost.setHeader("Cookie","Hm_lvt_cd8218cd51f800ed2b73e5751cb3f4f9=1576485981; adminUserName=admin; sidebarStatus=0; SESSION=NmRmMmVmYzMtMWYyNC00YmNhLWExOGQtNDViNzMwNTlkYzkx; Hm_lpvt_cd8218cd51f800ed2b73e5751cb3f4f9=1576487401");
            // 响应模型
            CloseableHttpResponse response = null;
            try {
                // 由客户端执行(发送)Post请求
                response = httpClient.execute(httpPost);
                // 从响应模型中获取响应实体
                HttpEntity responseEntity = response.getEntity();
                System.out.println(title+"响应状态为:" + response.getStatusLine());
                if (responseEntity != null) {
                    System.out.println("响应内容长度为:" + responseEntity.getContentLength());
                    System.out.println("响应内容为:" + EntityUtils.toString(responseEntity));
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
