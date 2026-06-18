import React from 'react'
import { Card, Tag, Avatar, Space, Button, Tooltip } from 'antd'
import {
  DownloadOutlined,
  StarOutlined,
  ShopOutlined,
  BulbOutlined,
  TeamOutlined,
  FileTextOutlined
} from '@ant-design/icons'
import { AppTemplate } from '@/api/template'
import { useNavigate } from 'react-router-dom'

interface TemplateCardProps {
  template: AppTemplate
  onInstall?: (template: AppTemplate) => void
  onView?: (template: AppTemplate) => void
  showActions?: boolean
  mode?: 'market' | 'my'
}

const categoryIcons: Record<string, React.ReactNode> = {
  oa: <TeamOutlined />,
  crm: <BulbOutlined />,
  inventory: <ShopOutlined />,
  business: <FileTextOutlined />,
  system: <FileTextOutlined />,
  other: <FileTextOutlined />,
}

const categoryColors: Record<string, string> = {
  oa: 'blue',
  crm: 'purple',
  inventory: 'green',
  business: 'cyan',
  system: 'geekblue',
  other: 'default',
}

const categoryNames: Record<string, string> = {
  oa: 'OA办公',
  crm: 'CRM客户',
  inventory: '进销存',
  business: '业务系统',
  system: '系统工具',
  other: '其他',
}

const typeLabels: Record<number, string> = {
  0: '官方',
  1: '用户',
  2: '团队',
}

const typeColors: Record<number, string> = {
  0: 'gold',
  1: 'blue',
  2: 'green',
}

const TemplateCard: React.FC<TemplateCardProps> = ({
  template,
  onInstall,
  onView,
  showActions = true,
  mode = 'market'
}) => {
  const navigate = useNavigate()

  const handleCardClick = () => {
    if (onView) {
      onView(template)
    }
  }

  const tagList = template.tags
    ? template.tags.split(',').filter(t => t.trim()).slice(0, 3)
    : []

  return (
    <Card
      hoverable
      onClick={handleCardClick}
      style={{
        borderRadius: 8,
        height: '100%',
        display: 'flex',
        flexDirection: 'column'
      }}
      bodyStyle={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        padding: 20
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
        <Avatar
          size={48}
          style={{
            backgroundColor: categoryColors[template.category || 'other']
              ? ''
              : '#1677ff',
            background: template.category
              ? `linear-gradient(135deg, ${categoryColors[template.category || 'other']}, #1677ff)`
              : '#1677ff',
            fontSize: 20
          }}
          icon={categoryIcons[template.category || 'other'] || <FileTextOutlined />}
        />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
            <h3 style={{
              margin: 0,
              fontSize: 16,
              fontWeight: 600,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap'
            }}>
              {template.templateName}
            </h3>
            {template.templateType !== undefined && (
              <Tag color={typeColors[template.templateType] || 'default'} style={{ margin: 0 }}>
                {typeLabels[template.templateType] || '未知'}
              </Tag>
            )}
          </div>
          <div style={{
            color: '#8c8c8c',
            fontSize: 12,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap'
          }}>
            {template.templateCode}
          </div>
        </div>
      </div>

      <div style={{
        color: '#595959',
        fontSize: 13,
        lineHeight: 1.6,
        marginBottom: 12,
        display: '-webkit-box',
        WebkitLineClamp: 2,
        WebkitBoxOrient: 'vertical',
        overflow: 'hidden',
        flex: 1,
        minHeight: 40
      }}>
        {template.templateDesc || '暂无描述'}
      </div>

      <div style={{ marginBottom: 12 }}>
        <Space wrap size={[4, 4]}>
          {tagList.map((tag, idx) => (
            <Tag key={idx} color="blue" style={{ margin: 0, fontSize: 11 }}>
              {tag}
            </Tag>
          ))}
        </Space>
      </div>

      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingTop: 12,
        borderTop: '1px solid #f0f0f0'
      }}>
        <Space size={16} style={{ color: '#8c8c8c', fontSize: 12 }}>
          <span>
            <DownloadOutlined style={{ marginRight: 4 }} />
            {template.installCount || 0}
          </span>
          <span>
            <StarOutlined style={{ marginRight: 4 }} />
            {template.starCount || 0}
          </span>
        </Space>
        <Space size={4}>
          <Tag color={categoryColors[template.category || 'other'] || 'default'} style={{ margin: 0, fontSize: 11 }}>
            {categoryNames[template.category || 'other'] || '其他'}
          </Tag>
        </Space>
      </div>

      {showActions && (
        <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
          <Button
            type="primary"
            size="small"
            block
            icon={<DownloadOutlined />}
            onClick={(e) => {
              e.stopPropagation()
              onInstall?.(template)
            }}
          >
            {mode === 'market' ? '一键安装' : '安装'}
          </Button>
          <Tooltip title="查看详情">
            <Button
              size="small"
              onClick={(e) => {
                e.stopPropagation()
                onView?.(template)
              }}
            >
              详情
            </Button>
          </Tooltip>
        </div>
      )}
    </Card>
  )
}

export default TemplateCard
