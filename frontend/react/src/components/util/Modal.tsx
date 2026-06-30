import { useUIContext } from '../../context/UIContext'

interface ModalProps {
  id: string
  title?: string
  children: React.ReactNode
}

export default function Modal({ id, title, children }: ModalProps) {
  const { activePanel, togglePanel } = useUIContext()
  const isOpen = activePanel === id

  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
      onClick={() => togglePanel(id)}
    >
      <div
        className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6"
        onClick={(e) => e.stopPropagation()}
      >
        {title && (
          <h2 className="text-base font-semibold text-gray-800 mb-4">{title}</h2>
        )}
        {children}
      </div>
    </div>
  )
}