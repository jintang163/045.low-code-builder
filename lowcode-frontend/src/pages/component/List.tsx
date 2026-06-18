import React, { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Input,
  Select,
  Tag,
  Modal,
  Form,
  Upload,
  message,
  Card,
  Descriptions,
  Timeline,
  Popconfirm,
  Badge,
} from 'antd'
import {
  PlusOutlined,
  UploadOutlined,
  DeleteOutlined,
  EyeOutlined,
  DownloadOutlined,
  ArrowUpOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { customComponentApi, CustomComponent, CustomComponentVersion } from '@/api/page'

const { Option } = Select
const { TextArea } = Input

const componentCategories = [
  { key: 'basic', name: '基础组件' },
  { key: 'form', name: '表单组件' },
  { key: 'layout', name: '布局容器' },
  { key: 'data', name: '数据展示' },
  { key: 'chart', name: '图表组件' },
  { key: 'advanced', name: '高级组件' },
  { key: 'custom', name: '自定义组件' },
]

const ComponentList: React.FC = () => {
  const [components, setComponents] = useState<CustomComponent[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })
  const [category, setCategory] = useState<string>('')
  const [keyword, setKeyword] = useState<string>('')
  const [uploadModalVisible, setUploadModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [versionModalVisible, setVersionModalVisible] = useState(false)
  const [selectedComponent, setSelectedComponent] = useState<CustomComponent | null>(null)
  const [uploadForm] = Form.useForm()
  const [versionForm] = Form.useForm()
  const [fileList, setFileList] = useState<UploadProps['fileList']>([])
  const [versionFileList, setVersionFileList] = useState<UploadProps['fileList']>([])
  const [submitting, setSubmitting] = useState(false)

  const loadComponents = async (page = 1, pageSize = 10) => {
    setLoading(true)
    try {
      const res = await customComponentApi.page(page, pageSize, category, keyword)
      if (res.data) {
        setComponents(res.data.records || [])
        setPagination({
          current: res.data.current || 1,
          pageSize: res.data.size || 10,
          total: res.data.total || 0,
        })
      }
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadComponents()
  }, [category, keyword])

  const handleUpload = async () => {
    try {
      const values = await uploadForm.validateFields()
      if (!fileList || fileList.length === 0) {
        message.warning('请选择组件包')
        return
      }

      setSubmitting(true)
      const formData = new FormData()
      formData.append('componentType', values.componentType)
      formData.append('componentName', values.componentName)
      formData.append('componentCategory', values.componentCategory)
      formData.append('icon', values.icon || '')
      formData.append('description', values.description || '')
      formData.append('author', values.author || '')
      formData.append('version', values.version)
      formData.append('changeLog', values.changeLog || '')
      formData.append('file', fileList[0].originFileObj as File)

      await customComponentApi.upload(formData)
      message.success('组件上传成功')
      setUploadModalVisible(false)
      uploadForm.resetFields()
      setFileList([])
      loadComponents(pagination.current, pagination.pageSize)
    } catch (e) {
      console.error(e)
      message.error('上传失败')
    } finally {
      setSubmitting(false)
    }
  }

  const handleUpdateVersion = async () => {
    try {
      const values = await versionForm.validateFields()
      if (!versionFileList || versionFileList.length === 0) {
        message.warning('请选择组件包')
        return
      }

      setSubmitting(true)
      const formData = new FormData()
      formData.append('componentId', String(selectedComponent?.id))
      formData.append('version', values.version)
      formData.append('changeLog', values.changeLog || '')
      formData.append('file', versionFileList[0].originFileObj as File)

      await customComponentApi.updateVersion(formData)
      message.success('版本更新成功')
      setVersionModalVisible(false)
      versionForm.resetFields()
      setVersionFileList([])
      loadComponents(pagination.current, pagination.pageSize)
    } catch (e) {
      console.error(e)
      message.error('更新失败')
    } finally {
      setSubmitting(false)
    }
  }

  const handleViewDetail = async (id: number) => {
    try {
      const res = await customComponentApi.get(id)
      if (res.data) {
        setSelectedComponent(res.data)
        setDetailModalVisible(true)
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await customComponentApi.delete(id)
      message.success('删除成功')
      loadComponents(pagination.current, pagination.pageSize)
    } catch (e) {
      console.error(e)
      message.error('删除失败')
    }
  }

  const handleDownload = async (componentType: string) => {
    try {
      const res = await customComponentApi.download(componentType)
      const url = window.URL.createObjectURL(new Blob([res.data as unknown as BlobPart]))
      const link = document.createElement('a')
      link.href = url
      link.download = `${componentType}.zip`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (e) {
      console.error(e)
      message.error('下载失败')
    }
  }

  const handleDeprecateVersion = async (versionId: number) => {
    try {
      await customComponentApi.deprecateVersion(versionId)
      message.success('已废弃该版本')
      if (selectedComponent) {
        handleViewDetail(selectedComponent.id!)
      }
    } catch (e) {
      console.error(e)
      message.error('操作失败')
    }
  }

  const columns = [
    {
      title: '组件图标',
      dataIndex: 'icon',
      width: 80,
      render: (icon: string) => <span style={{ fontSize: 24 }}>{icon || '📦'}</span>,
    },
    {
      title: '组件名称',
      dataIndex: 'componentName',
      width: 150,
    },
    {
      title: '组件类型',
      dataIndex: 'componentType',
      width: 150,
      render: (type: string) => <Tag color="blue">{type}</Tag>,
    },
    {
      title: '分类',
      dataIndex: 'componentCategory',
      width: 100,
      render: (cat: string) => {
        const category = componentCategories.find((c) => c.key === cat)
        return <Tag>{category?.name || cat}</Tag>
      },
    },
    {
      title: '当前版本',
      dataIndex: 'currentVersion',
      width: 100,
      render: (version: string) => (
        <Badge status="processing" text={`v${version}`} />
      ),
    },
    {
      title: '作者',
      dataIndex: 'author',
      width: 100,
    },
    {
      title: '描述',
      dataIndex: 'description',
      ellipsis: true,
    },
    {
      title: '操作',
      width: 240,
      fixed: 'right',
      render: (_: any, record: CustomComponent) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record.id!)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<ArrowUpOutlined />}
            onClick={() => {
              setSelectedComponent(record)
              setVersionModalVisible(true)
            }}
          >
            升级
          </Button>
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
            onClick={() => handleDownload(record.componentType)}
          >
            下载
          </Button>
          {record.isSystem !== 1 && (
            <Popconfirm
              title="确定删除该组件吗？"
              icon={<ExclamationCircleOutlined style={{ color: 'red' }} />}
              onConfirm={() => handleDelete(record.id!)}
            >
              <Button type="link" danger size="small" icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  const uploadProps: UploadProps = {
    fileList,
    beforeUpload: () => false,
    onChange: ({ fileList: newFileList }) => setFileList(newFileList),
    accept: '.zip',
    maxCount: 1,
  }

  const versionUploadProps: UploadProps = {
    fileList: versionFileList,
    beforeUpload: () => false,
    onChange: ({ fileList: newFileList }) => setVersionFileList(newFileList),
    accept: '.zip',
    maxCount: 1,
  }

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Space>
            <Input
              placeholder="搜索组件名称/类型"
              style={{ width: 250 }}
              allowClear
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={() => loadComponents()}
            />
            <Select
              placeholder="选择分类"
              style={{ width: 150 }}
              allowClear
              value={category || undefined}
              onChange={(value) => setCategory(value || '')}
            >
              {componentCategories.map((cat) => (
                <Option key={cat.key} value={cat.key}>
                  {cat.name}
                </Option>
              ))}
            </Select>
            <Button type="primary" onClick={() => loadComponents()}>
              搜索
            </Button>
          </Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setUploadModalVisible(true)}>
            上传组件
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={components}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            onChange: (page, pageSize) => loadComponents(page, pageSize),
          }}
        />
      </Card>

      <Modal
        title="上传自定义组件"
        open={uploadModalVisible}
        width={600}
        onOk={handleUpload}
        onCancel={() => {
          setUploadModalVisible(false)
          uploadForm.resetFields()
          setFileList([])
        }}
        confirmLoading={submitting}
        okText="上传"
      >
        <Form form={uploadForm} layout="vertical">
          <Form.Item
            name="componentType"
            label="组件类型"
            rules={[{ required: true, message: '请输入组件类型' }]}
          >
            <Input placeholder="例如：CustomButton" />
          </Form.Item>
          <Form.Item
            name="componentName"
            label="组件名称"
            rules={[{ required: true, message: '请输入组件名称' }]}
          >
            <Input placeholder="例如：自定义按钮" />
          </Form.Item>
          <Form.Item
            name="componentCategory"
            label="组件分类"
            rules={[{ required: true, message: '请选择组件分类' }]}
          >
            <Select placeholder="选择分类">
              {componentCategories.map((cat) => (
                <Option key={cat.key} value={cat.key}>
                  {cat.name}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="icon" label="组件图标">
            <Input placeholder="例如：🔘" maxLength={10} />
          </Form.Item>
          <Form.Item name="description" label="组件描述">
            <TextArea rows={3} placeholder="组件功能描述" />
          </Form.Item>
          <Form.Item name="author" label="作者">
            <Input placeholder="作者名称" />
          </Form.Item>
          <Form.Item
            name="version"
            label="版本号"
            rules={[{ required: true, message: '请输入版本号' }]}
          >
            <Input placeholder="例如：1.0.0" />
          </Form.Item>
          <Form.Item name="changeLog" label="更新说明">
            <TextArea rows={3} placeholder="版本更新说明" />
          </Form.Item>
          <Form.Item
            name="file"
            label="组件包"
            rules={[{ required: true, message: '请上传组件包' }]}
          >
            <Upload {...uploadProps}>
              <Button icon={<UploadOutlined />}>选择ZIP文件</Button>
            </Upload>
            <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
              支持ZIP格式，最大50MB
            </div>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="更新组件版本"
        open={versionModalVisible}
        width={500}
        onOk={handleUpdateVersion}
        onCancel={() => {
          setVersionModalVisible(false)
          versionForm.resetFields()
          setVersionFileList([])
        }}
        confirmLoading={submitting}
        okText="更新"
      >
        <Form form={versionForm} layout="vertical">
          <Form.Item label="组件名称">
            <Input value={selectedComponent?.componentName} disabled />
          </Form.Item>
          <Form.Item label="当前版本">
            <Input value={selectedComponent?.currentVersion} disabled />
          </Form.Item>
          <Form.Item
            name="version"
            label="新版本号"
            rules={[{ required: true, message: '请输入新版本号' }]}
          >
            <Input placeholder="例如：1.1.0" />
          </Form.Item>
          <Form.Item name="changeLog" label="更新说明">
            <TextArea rows={3} placeholder="版本更新说明" />
          </Form.Item>
          <Form.Item
            name="file"
            label="组件包"
            rules={[{ required: true, message: '请上传组件包' }]}
          >
            <Upload {...versionUploadProps}>
              <Button icon={<UploadOutlined />}>选择ZIP文件</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="组件详情"
        open={detailModalVisible}
        width={800}
        onCancel={() => setDetailModalVisible(false)}
        footer={null}
      >
        {selectedComponent && (
          <>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="组件名称">
                {selectedComponent.componentName}
              </Descriptions.Item>
              <Descriptions.Item label="组件类型">
                <Tag color="blue">{selectedComponent.componentType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="分类">
                {componentCategories.find((c) => c.key === selectedComponent.componentCategory)?.name}
              </Descriptions.Item>
              <Descriptions.Item label="当前版本">v{selectedComponent.currentVersion}</Descriptions.Item>
              <Descriptions.Item label="作者">{selectedComponent.author || '-'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {selectedComponent.status === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>}
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>
                {selectedComponent.description || '-'}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 16 }}>
              <h4>版本历史</h4>
              <Timeline>
                {selectedComponent.versions?.map((version: CustomComponentVersion) => (
                  <Timeline.Item
                    key={version.id}
                    color={version.status === 1 ? 'green' : 'gray'}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <strong>v{version.version}</strong>
                        {version.status === 1 && <Tag color="green" style={{ marginLeft: 8 }}>当前版本</Tag>}
                        {version.isDeprecated === 1 && <Tag color="orange" style={{ marginLeft: 8 }}>已废弃</Tag>}
                        <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                          {version.createdTime}
                        </div>
                        {version.changeLog && (
                          <div style={{ marginTop: 8, fontSize: 13 }}>{version.changeLog}</div>
                        )}
                      </div>
                      {version.status !== 1 && version.isDeprecated !== 1 && (
                        <Button
                          type="text"
                          size="small"
                          danger
                          onClick={() => handleDeprecateVersion(version.id!)}
                        >
                          废弃
                        </Button>
                      )}
                    </div>
                  </Timeline.Item>
                ))}
              </Timeline>
            </div>

            {selectedComponent.currentVersionInfo?.propSchema && (
              <div style={{ marginTop: 16 }}>
                <h4>属性Schema</h4>
                <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, maxHeight: 200, overflow: 'auto' }}>
                  {JSON.stringify(JSON.parse(selectedComponent.currentVersionInfo.propSchema), null, 2)}
                </pre>
              </div>
            )}

            {selectedComponent.currentVersionInfo?.exposedEvents && (
              <div style={{ marginTop: 16 }}>
                <h4>暴露事件</h4>
                <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, maxHeight: 200, overflow: 'auto' }}>
                  {JSON.stringify(JSON.parse(selectedComponent.currentVersionInfo.exposedEvents), null, 2)}
                </pre>
              </div>
            )}
          </>
        )}
      </Modal>
    </div>
  )
}

export default ComponentList
