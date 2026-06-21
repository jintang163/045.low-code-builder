import React from 'react'
import { Modal } from 'antd'

interface ConflictDialogProps {
  visible?: boolean
  onClose?: () => void
  onResolve?: (choice: string) => void
}

const ConflictDialog: React.FC<ConflictDialogProps> = ({ visible, onClose }) => {
  return (
    <Modal open={visible} onCancel={onClose}>
      <div>冲突解决</div>
    </Modal>
  )
}

export default ConflictDialog
