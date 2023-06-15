package com.bpm.activiti.modeler.common;


import java.util.List;

public class ResultListDataRepresentation
{
    protected Integer size;
    protected Long total;
    protected Integer start;
    protected List<? extends Object> data;

    public ResultListDataRepresentation() {}

    public ResultListDataRepresentation(List<? extends Object> data)
    {
        this.data = data;
        if (data != null)
        {
            this.size = Integer.valueOf(data.size());
            this.total = Long.valueOf(data.size());
            this.start = Integer.valueOf(0);
        }
    }

    public Integer getSize()
    {
        return this.size;
    }

    public void setSize(Integer size)
    {
        this.size = size;
    }

    public Long getTotal()
    {
        return this.total;
    }

    public void setTotal(Long total)
    {
        this.total = total;
    }

    public Integer getStart()
    {
        return this.start;
    }

    public void setStart(Integer start)
    {
        this.start = start;
    }

    public List<? extends Object> getData()
    {
        return this.data;
    }

    public void setData(List<? extends Object> data)
    {
        this.data = data;
    }
}
