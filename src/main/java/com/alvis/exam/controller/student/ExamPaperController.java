package com.alvis.exam.controller.student;

import com.alvis.exam.base.BaseApiController;
import com.alvis.exam.base.RestResponse;
import com.alvis.exam.domain.ExamPaper;
import com.alvis.exam.domain.ExamPaperAnswer;
import com.alvis.exam.domain.ExamPaperAnswerInfo;
import com.alvis.exam.domain.User;
import com.alvis.exam.event.CalculateExamPaperAnswerCompleteEvent;
import com.alvis.exam.service.ExamPaperAnswerService;
import com.alvis.exam.service.ExamPaperService;
import com.alvis.exam.utility.DateTimeUtil;
import com.alvis.exam.utility.ExamUtil;
import com.alvis.exam.utility.PageInfoHelper;
import com.alvis.exam.viewmodel.admin.exam.ExamPaperEditRequestVM;
import com.alvis.exam.viewmodel.admin.exam.ExamResponseVM;
import com.alvis.exam.viewmodel.student.exam.ExamPaperPageResponseVM;
import com.alvis.exam.viewmodel.student.exam.ExamPaperPageVM;
import com.alvis.exam.viewmodel.student.exam.ExamPaperReadVM;
import com.alvis.exam.viewmodel.student.exam.ExamPaperSubmitVM;
import com.github.pagehelper.PageInfo;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@RestController("StudentExamPaperController")
@RequestMapping(value = "/api/student/exam/paper")
@AllArgsConstructor
public class ExamPaperController extends BaseApiController {

    private final ExamPaperService examPaperService;
    private final ExamPaperAnswerService examPaperAnswerService;
    private final ApplicationEventPublisher eventPublisher;


    @RequestMapping(value = "/select/{id}", method = RequestMethod.POST)
    public RestResponse<ExamPaperEditRequestVM> select(@PathVariable Integer id) {
        User currentUser = getCurrentUser();
        ExamPaperEditRequestVM vm = examPaperService.examPaperToVM(id);
        return RestResponse.ok(vm);
    }


    @RequestMapping(value = "/insert", method = RequestMethod.POST)
    public void insert() throws Exception {
        int position=0;
        String[] bufstring=new String[1024];
        //打开带读取的文件
        BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\Administrator\\Desktop\\question.txt"));
        String line=null;
        while((line=br.readLine())!=null) {
            bufstring[position]=line;
            position++;
        }
        br.close();

    }





    @RequestMapping(value = "/pageList", method = RequestMethod.POST)
    public RestResponse<PageInfo<ExamPaperPageResponseVM>> pageList(@RequestBody @Valid ExamPaperPageVM model) {
        PageInfo<ExamPaper> pageInfo = examPaperService.studentPage(model);
        PageInfo<ExamPaperPageResponseVM> page = PageInfoHelper.copyMap(pageInfo, e -> {
            ExamPaperPageResponseVM vm = modelMapper.map(e, ExamPaperPageResponseVM.class);
            vm.setCreateTime(DateTimeUtil.dateFormat(e.getCreateTime()));
            return vm;
        });
        return RestResponse.ok(page);
    }

}
