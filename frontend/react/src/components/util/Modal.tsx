import { useUIContext } from '../../context/UIContext'

interface ModalProps {
  id: string
  children: React.ReactNode
}

export default function Modal({ id, children }: ModalProps) {
  const { activePanel } = useUIContext()
  const isOpen = activePanel === id

  if (!isOpen) return null

  return (
    <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-xl shadow-lg py-1 z-50">
      {children}
    </div>
  )
}