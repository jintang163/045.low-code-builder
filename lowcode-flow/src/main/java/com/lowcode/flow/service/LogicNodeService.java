package com.lowcode.flow.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.flow.entity.LogicNode;
import com.lowcode.flow.mapper.LogicNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogicNodeService extends ServiceImpl<LogicNodeMapper, LogicNode> {
}
